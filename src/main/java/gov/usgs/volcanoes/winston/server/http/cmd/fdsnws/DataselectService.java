/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import edu.iris.dmc.seedcodec.B1000Types;
import edu.iris.dmc.seedcodec.Steim2;
import edu.iris.dmc.seedcodec.SteimException;
import edu.iris.dmc.seedcodec.SteimFrameBlock;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import gov.usgs.plot.data.Wave;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint.FdsnConstraint;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * FDSN WS dataselect service.
 * 
 * @author Tom Parker
 *
 */
public class DataselectService extends FdsnwsService {

  private static final String VERSION = "1.1.2";
  private static final String SERVICE = "dataselect";

  private static final Logger LOGGER = LoggerFactory.getLogger(DataselectService.class);

  static {
    version = VERSION;
    service = SERVICE;
  }

  /**
   * Constructor.
   * 
   * @param databasePool winston database pool
   * @param ctx handler context
   * @param request the request
   * @throws UtilException when things go wrong
   * @throws FdsnException when FDSN-WS spec violated
   */
  public static void dispatch(WinstonDatabasePool databasePool, ChannelHandlerContext ctx,
      FullHttpRequest request) throws FdsnException, UtilException {
    String method = request.getUri().split("/")[4];

    switch (method) {
      case "version":
        sendVersion(ctx, request);
        break;
      case "application.wadl":
        sendWadl(ctx, request);
        break;
      case "query":
        sendQueryResponse(databasePool, ctx, request);
        break;
      default:
        ErrorResponse error = new ErrorResponse(ctx);
        error.request(request);
        error.version(VERSION);
        error.status(HttpResponseStatus.BAD_REQUEST);
        error.shortDescription("Bad request");
        error.detailedDescription("Request cannot be parsed.");
        error.sendError();
    }
  }

  private static void sendQueryResponse(WinstonDatabasePool databasePool, ChannelHandlerContext ctx,
      FullHttpRequest request) throws FdsnException, UtilException {
//    Map<String, String> arguments = parseRequest(request);
//
//    List<FdsnConstraint> constraints = buildConstraints(arguments);
//    LOGGER.debug("got constraints");
//
//    List<Channel> channels = getChannels(databasePool);
//    if (channels == null) {
//      ErrorResponse error = new ErrorResponse(ctx);
//      error.request(request);
//      error.version(VERSION);
//      error.status(HttpResponseStatus.NOT_FOUND);
//      error.shortDescription("No data");
//      error.detailedDescription("No matching data found.");
//      error.sendError();
//      return;
//    }
//
//    int seq = 1;
//    for (final Channel c : channels) {
//      if (pruneChannel(constraints, c)) {
//        continue;
//      }
//
//      Wave wave;
//      try {
//        wave = databasePool.doCommand(new WinstonConsumer<Wave>() {
//
//          public Wave execute(WinstonDatabase winston) throws UtilException {
//            Data data = new Data(winston);
//            return data.getWave(c.getSID(), st, et, 0);
//          }
//        });
//      } catch (Exception e1) {
//        throw new UtilException(e1.getMessage());
//      }
//
//
//      DataHeader header = new DataHeader(seq++, 'D', false);
//
//      header.setStationIdentifier(c.station);
//      header.setChannelIdentifier(c.channel);
//      header.setNetworkCode(c.network);
//      header.setLocationIdentifier(c.location);
//
//      header.setNumSamples((short) wave.numSamples());
//      header.setSampleRate(wave.getSamplingRate());
//      Btime btime = new Btime(Util.j2KToDate(wave.getStartTime()));
//      header.setStartBtime(btime);
//
//      DataRecord record = new DataRecord(header);
//
//      try {
//        Blockette1000 blockette1000 = new Blockette1000();
//        blockette1000.setEncodingFormat((byte) B1000Types.STEIM2);
//        blockette1000.setWordOrder((byte) 1);
//
//        // todo:fix this
//        // pad data or split
//        blockette1000
//            .setDataRecordLength((byte) (Math.ceil(Math.log(wave.buffer.length * 5) / Math.log(2))));
//
//        record.addBlockette(blockette1000);
//
//        SteimFrameBlock data = null;
//
//        try {
//          data = Steim2.encode(wave.buffer, 63);
//          record.setData(data.getEncodedData());
//          // record.write(dos);
//        } catch (SteimException e) {
//          e.printStackTrace();
//        }
//      } catch (SeedFormatException e) {
//        e.printStackTrace();
//      } finally {
//      }
//    }
  }
}
