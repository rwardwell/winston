package gov.usgs.volcanoes.winston.in.ew;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.legacy.ew.Menu;
import gov.usgs.volcanoes.core.legacy.ew.MenuItem;
import gov.usgs.volcanoes.core.legacy.ew.WaveServer;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 *
 * @author Dan Cervelli
 */
public class ImportWS {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportWS.class);

  private static final String DEFAULT_CONFIG_FILENAME = "ImportWS.config";
  private static final double DEFAULT_CHUNK_SIZE = 600.0;
  private static final int DEFAULT_CHUNK_DELAY = 500;

  private static final boolean DEFAULT_RSAM_ENABLE = true;
  private static final int DEFAULT_RSAM_DELTA = 10;
  private static final int DEFAULT_RSAM_DURATION = 60;

  private WaveServer waveServer;
  private WinstonDatabase winston;

  private Channels channels;
  private Data data;
  private Menu menu;

  private ConfigFile config;

  private boolean createChannels;
  private boolean createDatabase;

  private List<ImportWSJob> jobs;
  private ImportWSJob currentJob;
  private List<String> sourceChannels;

  private TimeSpan timeSpan;
  // private double startTime;
  // private double endTime;

  private double chunkSize;
  private int chunkDelay;

  private boolean rsamEnable;
  private int rsamDelta;
  private int rsamDuration;

  private int totalInserted = 0;
  private double totalDownloadTime = 0;
  private double totalInsertTime = 0;

  private boolean requestSCNL = false;

  private boolean quit = false;

  private final CodeTimer appTimer;

  // JSAP related stuff.
  public static String JSAP_PROGRAM_NAME = "java gov.usgs.volcanoes.winston.in.ew.ImportWS";
  public static String JSAP_EXPLANATION_PREFACE = "Winston ImportWS\n" + "\n"
      + "This program gets data from a Winston wave server and imports\n"
      + "it into a Winston database. See 'ImportWS.config' for more options.\n" + "\n";

  private static final String DEFAULT_JSAP_EXPLANATION = "All output goes to standard error.\n"
      + "The command line takes precedence over the config file.\n";

  private static final Parameter[] DEFAULT_JSAP_PARAMETERS = new Parameter[] {
      new FlaggedOption("timeRange", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 't',
          "timerange", "The time range. Relative times are assumed to be in the past.\n"),
      new FlaggedOption("waveServer", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'w',
          "waveserver", "The Winston wave server to poll.\n").setUsageName("host:port"),
      new Switch("noInput", 'i', "noinput", "Do not poll keyboard for input."),
      new Switch("SCNL", 'l', "SCNL", "Always request SCNL"),
      new UnflaggedOption("configFilename", JSAP.STRING_PARSER, DEFAULT_CONFIG_FILENAME,
          JSAP.REQUIRED, JSAP.NOT_GREEDY, "The config file name.")};

  public ImportWS() {
    appTimer = new CodeTimer("application");
  }

  public ImportWS(final String fileName) throws ParseException {
    this();
    config = new ConfigFile(fileName);
    processConfig();
  }

  public void setConfig(final ConfigFile config) {
    this.config = config;
  }

  public void setRequestSCNL(final boolean requestSCNL) {
    this.requestSCNL = requestSCNL;
  }

  public boolean getRequestSCNL() {
    return requestSCNL;
  }

  public void processConfig() throws ParseException {
    createDatabase = StringUtils.stringToBoolean(config.getString("createDatabase"));
    LOGGER.info("createDatabase: " + createDatabase);

    winston = WinstonDatabase.processWinstonConfigFile(config);
    LOGGER.info("winston.driver: " + winston.dbDriver);
    LOGGER.info("winston.url: " + winston.dbURL);
    LOGGER.info("winston.prefix: " + winston.databasePrefix);
    LOGGER.info("winston.statementCacheCap: " + winston.cacheCap);

    if (createDatabase) {
      winston.checkDatabase();
    }

    final String s = config.getString("waveServer");
    if (s == null)
      throw new RuntimeException("no waveServer string");

    waveServer = new WaveServer(config.getString("waveServer"));
    LOGGER.info("waveServer: {}:{}", waveServer.host, waveServer.port);

    createChannels = StringUtils.stringToBoolean(config.getString("createChannels"));
    LOGGER.info("createChannels: {}", createChannels);

    sourceChannels = config.getList("channel");
    LOGGER.info("sourceChannels: {}", sourceChannels);

    final String timeRange = config.getString("timeRange");
    LOGGER.info("timeRange: {}", timeRange);
    setTimeRange(timeRange);

    chunkSize = StringUtils.stringToDouble(config.getString("chunkSize"), DEFAULT_CHUNK_SIZE);
    LOGGER.info("chunkSize: {}", chunkSize);

    chunkDelay = StringUtils.stringToInt(config.getString("chunkDelay"), DEFAULT_CHUNK_DELAY);
    LOGGER.info("chunkDelay: {}", chunkDelay);

    rsamEnable = StringUtils.stringToBoolean(config.getString("rsam.enable"), DEFAULT_RSAM_ENABLE);
    LOGGER.info("rsamEnable: {}", rsamEnable);

    rsamDelta = StringUtils.stringToInt(config.getString("rsam.delta"), DEFAULT_RSAM_DELTA);
    LOGGER.info("rsamDelta: {}", rsamDelta);

    rsamDuration =
        StringUtils.stringToInt(config.getString("rsam.duration"), DEFAULT_RSAM_DURATION);
    LOGGER.info("rsamDuration: {}", rsamDuration);
    // TODO: log level
  }

  public void addStats(final int t, final double td, final double ti) {
    totalInserted += t;
    totalDownloadTime += td;
    totalInsertTime += ti;
  }

  // private void parseTimeRange(final String timeRange) {
  // try {
  // final double[] tr = Time.parseTimeRange(timeRange);
  // startTime = tr[0];
  // endTime = tr[1];
  // } catch (final Exception e) {
  // throw new RuntimeException("Error parsing time range: " + e.getMessage());
  // }
  //
  // LOGGER
  // .info(String.format("Requested time range: [%s -> %s, %s]", J2kSec.toDateString(startTime),
  // J2kSec.toDateString(endTime), Time.secondsToString(endTime - startTime)));
  // }

  public void setWinston(final WinstonDatabase w) {
    winston = w;
  }

  public void setWaveServer(final WaveServer ws) {
    waveServer = ws;
  }

  private void getChannels() {
    channels = new Channels(winston);
    data = new Data(winston);
    waveServer.connect();
    menu = waveServer.getMenuSCNL();
    waveServer.close();
  }

  public Menu getMenu() {
    return menu;
  }

  public void createJobs() {
    getChannels();
    jobs = new ArrayList<ImportWSJob>();

    final List<MenuItem> items = menu.getItems();
    for (final MenuItem item : items) {
      for (final String channel : sourceChannels) {
        final String[] ss = channel.split("[\\$\\_ ]");
        String loc = null;
        if (ss.length == 4)
          loc = ss[3];

        if (item.match(ss[0], ss[1], ss[2], loc)) {
          final String wc = item.getSCNSCNL("$");

          if (!createChannels && !channels.channelExists(wc))
            continue;

          LOGGER.info("Remote channel matched: {}", wc);
          final ImportWSJob job = new ImportWSJob(winston, waveServer, this);
          job.setChannel(wc);
          job.setChunkDelay(chunkDelay);
          job.setChunkSize(chunkSize);
          job.setRSAMParameters(rsamEnable, rsamDelta, rsamDuration);
          jobs.add(job);
        }
      }
    }
  }

  public void startImport() {
    for (final ImportWSJob job : jobs) {
      currentJob = job;
      LOGGER.info("{}: finding gaps", job.getChannel());
      final List<TimeSpan> gaps = data.findGaps(job.getChannel(), timeSpan);
      job.addSpans(gaps);

      job.go();

      if (quit)
        break;
    }
    appTimer.stop();
    LOGGER.info(
        String.format("%d tbs inserted, total download time: %s, total insert time: %s (%.3fms/tb)",
            totalInserted, Time.secondsToString(totalDownloadTime / 1000),
            Time.secondsToString(totalInsertTime / 1000), totalInsertTime / totalInserted));
    LOGGER.info("Total run time: " + Time.secondsToString(appTimer.getRunTimeMillis() / 1000.0));
    quit = true;
  }

  public void go() {
    final Thread launchThread = new Thread(new Runnable() {
      public void run() {
        startImport();
      }
    });
    launchThread.start();
  }

  public void quit() {
    LOGGER.info("Quitting cleanly.");
    if (currentJob != null)
      currentJob.quit();
    else
      LOGGER.info("Null job");
    quit = true;
  }

  /**
   * Find and parse the command line arguments.
   *
   * @param args The command line arguments.
   * @throws JSAPException 
   */
  private static JSAPResult getArguments(final String[] args) throws JSAPException {
    JSAPResult config = null;
    final SimpleJSAP jsap = new SimpleJSAP(JSAP_PROGRAM_NAME,
        JSAP_EXPLANATION_PREFACE + DEFAULT_JSAP_EXPLANATION, DEFAULT_JSAP_PARAMETERS);

    config = jsap.parse(args);

    if (jsap.messagePrinted()) {
      // The following error message is useful for catching the case
      // when args are missing, but help isn't printed.
      if (!config.getBoolean("help"))
        throw new RuntimeException("Try using the --help flag.");
    }

    return config;
  }

  private void setTimeRange(String timeRange) throws ParseException {
    timeSpan = TimeSpan.parse(timeRange);
  }

  public void setQuit(final boolean b) {
    quit = b;
  }

  public static void main(final String[] args) throws IOException, JSAPException, ParseException {
    final JSAPResult config = getArguments(args);
    final ImportWS w = new ImportWS(config.getString("configFilename"));

    if (config.getString("timeRange") != null) {
      w.setTimeRange(config.getString("timeRange"));
    }

    if (config.getString("waveServer") != null)
      w.waveServer = new WaveServer(config.getString("waveServer"));

    final boolean acceptCommands = !(config.getBoolean("noInput"));

    w.setRequestSCNL((config.getBoolean("SCNL")));

    w.createJobs();
    w.go();
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (acceptCommands && !w.quit) {
      String s = in.readLine();
      if (s != null) {
        s = s.toLowerCase().trim();
        if (s.equals("q"))
          w.quit();
      }
    }
  }
}
