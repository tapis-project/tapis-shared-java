package edu.utexas.tacc.tapis.shared.threadlocal;

import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * OrderBy information for a single attribute including attribute name and sort direction.
 * Immutable. Attribute must be non-empty. Direction defaults to ascending.
 */
public final class OrderBy
{
  public enum OrderByDir {ASC, DESC}
  public static final OrderByDir DEFAULT_ORDERBY_DIRECTION = OrderByDir.ASC;

  private final String orderByAttr;
  private final OrderByDir orderByDir;

  public OrderBy(String attr, OrderByDir direction)
  {
    if (StringUtils.isBlank(attr)) throw new IllegalArgumentException(MsgUtils.getMsg("TAPIS_QUERY_PARAM_ORDERBY_NOATTR"));
    orderByAttr = attr;
    orderByDir = (direction == null ? DEFAULT_ORDERBY_DIRECTION : direction);
  }
  public String getOrderByAttr() { return orderByAttr; }
  public OrderByDir getOrderByDir() { return orderByDir; }
}
