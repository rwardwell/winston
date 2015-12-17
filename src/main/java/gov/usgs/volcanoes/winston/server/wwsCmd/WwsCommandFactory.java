/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;

/**
 * 
 * @author Tom Parker
 *
 */
public enum WwsCommandFactory {
  MENU("MENU", MenuCommand.class),
  VERSION("VERSION", VersionCommand.class),
  GETCHANNELS("GC", GetChannelsCommand.class),

  //  STATUS("STATUS", StatusCommand.class),
//GETSCNRAW("GETSCNRAW", StatusCommand.class),
//GETSCNLRAW("GETSCNLRAW", StatusCommand.class),
//GETSCN("GETSCN", StatusCommand.class),
//GETSCNL("GETSCNL", StatusCommand.class),
//GETSCNLHELIRAW("GETSCNLHELIRAW", StatusCommand.class),
//GETSCNLRSAMRAW("GETSCNLRSAMRAW", StatusCommand.class),
//GETWAVERAW("GETWAVERAW", StatusCommand.class),
//GETMETADATA("GETMETADATA", StatusCommand.class),
 ;

  private static final Logger LOGGER = LoggerFactory.getLogger(WwsCommandFactory.class);

  private String command;
  private Class<? extends WwsBaseCommand> clazz;

  private WwsCommandFactory(String command, Class<? extends WwsBaseCommand> clazz) {
    this.command = command;
    this.clazz = clazz;
  }

  /**
   * 
   * @param winstonDatabasePool 
   * @param command
   * @return
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws UnsupportedCommandException 
   */
  public static WwsBaseCommand get(WinstonDatabasePool databasePool, WwsCommandString command)
      throws InstantiationException, IllegalAccessException, UnsupportedCommandException {
    for (WwsCommandFactory cmd : WwsCommandFactory.values()) {
      if (cmd.command.equals(command.getCommand())) {
        WwsBaseCommand baseCommand = cmd.clazz.newInstance();
        baseCommand.databasePool(databasePool);
        return baseCommand;
      }
    }
    throw new UnsupportedCommandException("Unknown WWS command " + command.getCommand());
  }
}
