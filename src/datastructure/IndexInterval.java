/**
 * Provide definition for index interval
 */

package datastructure;

/**
 * A class that defines index interval for the interval tree
 * 
 * @author Haoran Sun
 * @since 01/26/2017
 */
public class IndexInterval implements Interval {
  private int start, end;

  /**
   * Constructor
   * 
   * @param start - start point
   * @param end - end point
   */
  public IndexInterval(int start, int end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Getter for the start point
   * 
   * @return the start point
   */
  @Override
  public int start() {
    return this.start;
  }

  /**
   * Getter for the end point
   * 
   * @return the end point
   */
  @Override
  public int end() {
    return this.end;
  }

  /**
   * Convert information to string
   * 
   * @return a string that represents interval
   */
  @Override
  public String toString() {
    return "[" + this.start + ", " + this.end + "]";
  }
}