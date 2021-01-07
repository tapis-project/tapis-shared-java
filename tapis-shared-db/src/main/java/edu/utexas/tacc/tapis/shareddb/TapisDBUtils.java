package edu.utexas.tacc.tapis.shareddb;

import java.util.List;

public final class TapisDBUtils 
{
    /** Create the aloe database url with the given parameters.
     * 
     * @param host db host
     * @param port db port
     * @param database schema name
     * @return the jdbc url
     */
    public static String makeJdbcUrl(String host, int port, String database) 
    {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
    
    /** SQL queries on string fields can contain wildcard characters (% and _).
     * This method escapes those characters to avoid having them interpreted 
     * by SQL as wildcards.
     * 
     * @param s the string that might contain wildcards
     * @return the string with wildcards escaped
     */
    public static String escapeSqlWildcards(String s)
    {
        s = s.replace("%", "\\%");
        s = s.replace("_", "\\_");
        return s;
    }
    
    /** Convert a list of string into a string with the format:
     * 
     *      ('item1', 'item2', ...)
     *      
     * Null is returned if the list is null or empty.     
     * 
     * @param items list of strings
     * @return a string in sql format or null
     */
    public static String makeSqlList(List<String> items)
    {
        // We accept everything.
        if (items == null || items.isEmpty()) return null;
        
        // Create the a buffer initialized to some length
        // proportional to the number of list items.
        var buf = new StringBuilder(items.size() * 10);
        boolean firstItem = true;
        buf.append("(");
        for (String item : items) {
            if (!firstItem) buf.append(", ");
              else firstItem = false;
            buf.append("'");
            buf.append(item);
            buf.append("'");
        }
        buf.append(")");
        
        return buf.toString();
    }
}
