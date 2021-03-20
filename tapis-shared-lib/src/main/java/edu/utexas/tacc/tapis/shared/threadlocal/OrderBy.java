package edu.utexas.tacc.tapis.shared.threadlocal;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * OrderBy information for a single attribute including attribute name and order direction.
 * Immutable. Attribute must be non-empty. Direction defaults to ascending.
 */
public final class OrderBy
{
  public enum OrderByDir {ASC, DESC}
  public static final OrderByDir DEFAULT_ORDERBY_DIRECTION = OrderByDir.ASC;

  private final String orderByAttr;
  private final OrderByDir orderByDir;

  // Private constructor for use by valueOf, fromString. Direction defaults to ASC
  private OrderBy(String attr, OrderByDir direction)
  {
    orderByAttr = attr;
    orderByDir = (direction == null ? DEFAULT_ORDERBY_DIRECTION : direction);
  }

  // Accessors
  public String getOrderByAttr() { return orderByAttr; }
  public OrderByDir getOrderByDir() { return orderByDir; }

  /**
   * Static factory method to create an instance
   *
   * @param attr Attribute name. Must be non-blank.
   * @param direction Order direction. If null defaults to ASC
   * @return new instance of OrderBy
   * @throws IllegalArgumentException If attribute name is empty or null
   */
  public static OrderBy valueOf(String attr, OrderByDir direction) throws IllegalArgumentException
  {
    if (StringUtils.isBlank(attr)) throw new IllegalArgumentException(MsgUtils.getMsg("TAPIS_ORDERBY_ENTRY_NOATTR"));
    return new OrderBy(StringUtils.trim(attr), direction);
  }

  /**
   * Static factory method to create a new instance of OrderBy from the string representation.
   * String representation must have the format attribute_name(direction). E.g. name(asc) or created(desc)
   * If direction is not present or is missing (e.g. name()) then direction defaults to ASC.
   *
   * @param orderByStr String representation of OrderBy in format attribute(direction)
   * @return new instance of OrderBy
   * @throws IllegalArgumentException If string representation is invalid
   */
  public static OrderBy fromString(String orderByStr) throws IllegalArgumentException
  {
    if (StringUtils.isBlank(orderByStr))
        throw new IllegalArgumentException(MsgUtils.getMsg("TAPIS_ORDERBY_ENTRY_INVALID", orderByStr));
    orderByStr = StringUtils.trim(orderByStr);
    // Make sure we do not have an unmatched paren
    int orderDirStart = orderByStr.indexOf('(');
    if (orderDirStart > 0 && !orderByStr.endsWith(")"))
      throw new IllegalArgumentException(MsgUtils.getMsg("TAPIS_ORDERBY_ENTRY_INVALID", orderByStr));

    // Extract attribute name
    String attrName = orderByStr;
    if (orderDirStart >= 0) attrName =  orderByStr.substring(0,orderDirStart);
    if (StringUtils.isBlank(attrName))
      throw new IllegalArgumentException(MsgUtils.getMsg("TAPIS_ORDERBY_ENTRY_INVALID", orderByStr));

    // Extract order direction
    OrderByDir orderByDir = DEFAULT_ORDERBY_DIRECTION;
    if (orderDirStart > 0)
    {
      String orderDirStr = orderByStr.substring(orderDirStart+1, orderByStr.length()-1).toUpperCase();
      if (!StringUtils.isBlank(orderDirStr)) orderByDir = OrderByDir.valueOf(orderDirStr);
    }
    return new OrderBy(attrName, orderByDir);
  }

  /**
   * Returns the string representation of this OrderBy instance in the format
   *   attribute_name(direction). E.g. name(asc) or created(desc)
   * @return String representation of the OrderBy entry in format attr(dir)
   */
  @Override
  public String toString()
  {
    return String.format("%s(%s)", orderByAttr, orderByDir.name().toLowerCase());
  }
}
