/**
 * Modified by Haoran Sun
 * @since 01/26/2017
 */
package datastructure;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A balanced binary-search tree keyed by Interval objects.
 *
 * The underlying data-structure is a red-black tree largely implemented from
 * CLRS (Introduction to Algorithms, 2nd edition) with the interval-tree
 * extensions mentioned in section 14.3
 * 
 * Restrictions apply: all intervals are disjoint, and this class is optimized
 * to support algorithm used in the document tagger so that it may not work for
 * most overlapping interval queries.
 * 
 * @param - the type of Interval this tree contains
 */
public class IntervalTree<T extends Interval> implements Iterable<T> {

  private Node root; // The root Node.
  private Node nil; // The sentinel Node to represent the absence of a node.
  private int size; // Size of the tree. Updated by insert() and Node#delete()

  /**
   * Constructs an empty IntervalTree.
   */
  public IntervalTree() {
    nil = new Node();
    root = nil;
    size = 0;
  }

  /**
   * Constructs an IntervalTree containing a single node corresponding to the
   * given interval.
   * 
   * @param t - the interval to add to the tree
   */
  public IntervalTree(T t) {
    nil = new Node();
    root = new Node(t);
    root.blacken();
    size = 1;
  }

  ///////////////////////////////////
  // Tree -- General query methods //
  ///////////////////////////////////

  /**
   * Whether this IntervalTree is empty or not.
   */
  public boolean isEmpty() {
    return root.isNil();
  }

  /**
   * The number of intervals stored in this IntervalTree.
   */
  public int size() {
    return size;
  }

  /**
   * The Node in this IntervalTree that contains the given Interval.
   * <p>
   * This method returns the nil Node if the Interval t cannot be found.
   * 
   * @param t - the Interval to search for.
   */
  private Node search(T t) {
    return root.search(t);
  }

  /**
   * Whether or not this IntervalTree contains the given Interval.
   * 
   * @param t - the Interval to search for
   */
  public boolean contains(T t) {
    return !search(t).isNil();
  }

  /**
   * The minimum value in this IntervalTree
   * 
   * @return an Optional containing, if it exists, the minimum value in this
   *         IntervalTree; otherwise (i.e., if this is empty), an empty
   *         Optional.
   */
  public Optional<T> minimum() {
    Node n = root.minimumNode();
    return n.isNil() ? Optional.empty() : Optional.of(n.interval());
  }

  /**
   * The maximum value in this IntervalTree
   * 
   * @return an Optional containing, if it exists, the maximum value in this
   *         IntervalTree; otherwise (i.e., if this is empty), an empty
   *         Optional.
   */
  public Optional<T> maximum() {
    Node n = root.maximumNode();
    return n.isNil() ? Optional.empty() : Optional.of(n.interval());
  }

  /**
   * The next Interval in this IntervalTree
   * 
   * @param t - the Interval to search for
   * @return an Optional containing, if it exists, the next Interval in this
   *         IntervalTree; otherwise (if t is the maximum Interval, or if this
   *         IntervalTree does not contain t), an empty Optional.
   */
  public Optional<T> successor(T t) {
    Node n = search(t);
    if (n.isNil()) {
      return Optional.empty();
    }

    n = n.successor();
    if (n.isNil()) {
      return Optional.empty();
    }

    return Optional.of(n.interval());
  }

  /**
   * The previous Interval in this IntervalTree
   * 
   * @param t - the Interval to search for
   * @return an Optional containing, if it exists, the previous Interval in this
   *         IntervalTree; otherwise (if t is the minimum Interval, or if this
   *         IntervalTree does not contain t), an empty Optional.
   */
  public Optional<T> predecessor(T t) {
    Node n = search(t);
    if (n.isNil()) {
      return Optional.empty();
    }

    n = n.predecessor();
    if (n.isNil()) {
      return Optional.empty();
    }

    return Optional.of(n.interval());
  }

  /**
   * An Iterator which traverses the tree in ascending order.
   */
  public Iterator<T> iterator() {
    return new TreeIterator(root);
  }

  ///////////////////////////////
  // Tree -- Insertion methods //
  ///////////////////////////////

  /**
   * Inserts the given value into the IntervalTree.
   * <p>
   * This method constructs a new Node containing the given value and places it
   * into the tree. If the value already exists within the tree, the tree
   * remains unchanged.
   * 
   * @param t - the value to place into the tree
   * @return if the value did not already exist, i.e., true if the tree was
   *         changed, false if it was not
   */
  public boolean insert(T t) {

    Node z = new Node(t);
    Node y = nil;
    Node x = root;

    while (!x.isNil()) { // Traverse the tree down to a leaf.
      y = x;
      x.maxEnd = Math.max(x.maxEnd, z.maxEnd); // Update maxEnd on the way down.
      int cmp = z.compareTo(x);
      if (cmp == 0) {
        return false; // Value already in tree. Do nothing.
      }
      x = cmp == -1 ? x.left : x.right;
    }

    z.parent = y;

    if (y.isNil()) {
      root = z;
      root.blacken();
    } else { // Set the parent of n.
      int cmp = z.compareTo(y);
      if (cmp == -1) {
        y.left = z;
      } else {
        assert (cmp == 1);
        y.right = z;
      }

      z.left = nil;
      z.right = nil;
      z.redden();
      z.insertFixup();
    }

    size++;
    return true;
  }

  /**
   * Find the next available (possibly) start point for a given interval
   * 
   * @param interV interval to query
   * @return the given start point if there is no overlap; otherwise return the
   *         next available (possibly) position.
   */
  public int nextAvailable(IndexInterval interV) {
    if (root == null)
      return 0;
    IndexInterval interval = new IndexInterval(interV.end(), interV.end());
    Node curr = root;
    while (curr.interval() != null) {
      if (curr.interval().overlaps(interval))
        return curr.interval().end() + 1;
      if (interV.end() < curr.interval().start())
        curr = curr.left;
      else
        curr = curr.right;
    }
    return interV.start();
  }

  /**
   * Perform in-order traversal
   */
  public void inOrder() {
    this.inOrder(root);
  }

  /**
   * Helper method for in-order traversal
   * 
   * @param root node to traverse from
   */
  private void inOrder(Node root) {
    if (root.left.interval != null)
      inOrder(root.left);
    System.out.println(root.interval);
    if (root.right.interval != null)
      inOrder(root.right);
  }

  /**
   * A representation of a node in an interval tree.
   */
  private class Node implements Interval {

    /*
     * Most of the "guts" of the interval tree are actually methods called by
     * nodes. For example, IntervalTree#delete(val) searches up the Node
     * containing val; then that Node deletes itself with Node#delete().
     */

    private T interval;
    private Node parent;
    private Node left;
    private Node right;
    private boolean isBlack;
    private int maxEnd;

    /**
     * Constructs a Node with no data.
     * <p>
     * This Node has a null interval field, is black, and has all pointers
     * pointing at itself. This is intended to be used as the sentinel node in
     * the tree ("nil" in CLRS).
     */
    private Node() {
      parent = this;
      left = this;
      right = this;
      blacken();
    }

    /**
     * Constructs a Node containing the given Interval.
     * 
     * @param data - the Interval to be contained within this Node
     */
    public Node(T interval) {
      this.interval = interval;
      parent = nil;
      left = nil;
      right = nil;
      maxEnd = interval.end();
      redden();
    }

    /**
     * The Interval in this Node
     */
    public T interval() {
      return interval;
    }

    /**
     * The start of the Interval in this Node
     */
    @Override
    public int start() {
      return interval.start();
    }

    /**
     * The end of the Interval in this Node
     */
    @Override
    public int end() {
      return interval.end();
    }

    ///////////////////////////////////
    // Node -- General query methods //
    ///////////////////////////////////

    /**
     * Searches the subtree rooted at this Node for the given Interval.
     * 
     * @param t - the Interval to search for
     * @return the Node with the given Interval, if it exists; otherwise, the
     *         sentinel Node
     */
    private Node search(T t) {

      Node n = this;

      while (!n.isNil() && t.compareTo(n) != 0) {
        n = t.compareTo(n) == -1 ? n.left : n.right;
      }
      return n;
    }

    /**
     * Searches the subtree rooted at this Node for its minimum Interval.
     * 
     * @return the Node with the minimum Interval, if it exists; otherwise, the
     *         sentinel Node
     */
    private Node minimumNode() {

      Node n = this;

      while (!n.left.isNil()) {
        n = n.left;
      }
      return n;
    }

    /**
     * Searches the subtree rooted at this Node for its maximum Interval.
     * 
     * @return the Node with the maximum Interval, if it exists; otherwise, the
     *         sentinel Node
     */
    private Node maximumNode() {

      Node n = this;

      while (!n.right.isNil()) {
        n = n.right;
      }
      return n;
    }

    /**
     * The successor of this Node.
     * 
     * @return the Node following this Node, if it exists; otherwise the
     *         sentinel Node
     */
    private Node successor() {

      if (!right.isNil()) {
        return right.minimumNode();
      }

      Node x = this;
      Node y = parent;
      while (!y.isNil() && x == y.right) {
        x = y;
        y = y.parent;
      }

      return y;
    }

    /**
     * The predecessor of this Node.
     * 
     * @return the Node preceding this Node, if it exists; otherwise the
     *         sentinel Node
     */
    private Node predecessor() {

      if (!left.isNil()) {
        return left.maximumNode();
      }

      Node x = this;
      Node y = parent;
      while (!y.isNil() && x == y.left) {
        x = y;
        y = y.parent;
      }

      return y;
    }

    ////////////////////////////////////////////////
    // Node -- Tree-invariant maintenance methods //
    ////////////////////////////////////////////////

    /**
     * Whether or not this Node is the root of its tree.
     */
    public boolean isRoot() {
      return (!isNil() && parent.isNil());
    }

    /**
     * Whether or not this Node is the sentinel node.
     */
    public boolean isNil() {
      return this == nil;
    }

    /**
     * Whether or not this Node is the left child of its parent.
     */
    public boolean isLeftChild() {
      return this == parent.left;
    }

    /**
     * Whether or not this Node is the right child of its parent.
     */
    public boolean isRightChild() {
      return this == parent.right;
    }

    /**
     * Whether or not this Node has no children, i.e., is a leaf.
     */
    public boolean hasNoChildren() {
      return left.isNil() && right.isNil();
    }

    /**
     * Whether or not this Node has two children, i.e., neither of its children
     * are leaves.
     */
    public boolean hasTwoChildren() {
      return !left.isNil() && !right.isNil();
    }

    /**
     * Sets this Node's color to black.
     */
    private void blacken() {
      isBlack = true;
    }

    /**
     * Sets this Node's color to red.
     */
    private void redden() {
      isBlack = false;
    }

    /**
     * Whether or not this Node's color is red.
     */
    public boolean isRed() {
      return !isBlack;
    }

    /**
     * A pointer to the grandparent of this Node.
     */
    private Node grandparent() {
      return parent.parent;
    }

    /**
     * Sets the maxEnd value for this Node.
     * <p>
     * The maxEnd value should be the highest of:
     * <ul>
     * <li>the end value of this node's data
     * <li>the maxEnd value of this node's left child, if not null
     * <li>the maxEnd value of this node's right child, if not null
     * </ul>
     * <p>
     * This method will be correct only if the left and right children have
     * correct maxEnd values.
     */
    private void resetMaxEnd() {
      int val = interval.end();
      if (!left.isNil()) {
        val = Math.max(val, left.maxEnd);
      }
      if (!right.isNil()) {
        val = Math.max(val, right.maxEnd);
      }
      maxEnd = val;
    }

    /**
     * Performs a left-rotation on this Node.
     * 
     * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
     */
    private void leftRotate() {
      Node y = right;
      right = y.left;

      if (!y.left.isNil()) {
        y.left.parent = this;
      }

      y.parent = parent;

      if (parent.isNil()) {
        root = y;
      } else if (isLeftChild()) {
        parent.left = y;
      } else {
        parent.right = y;
      }

      y.left = this;
      parent = y;

      resetMaxEnd();
      y.resetMaxEnd();
    }

    /**
     * Performs a right-rotation on this Node.
     * 
     * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
     */
    private void rightRotate() {
      Node y = left;
      left = y.right;

      if (!y.right.isNil()) {
        y.right.parent = this;
      }

      y.parent = parent;

      if (parent.isNil()) {
        root = y;
      } else if (isLeftChild()) {
        parent.left = y;
      } else {
        parent.right = y;
      }

      y.right = this;
      parent = y;

      resetMaxEnd();
      y.resetMaxEnd();
    }

    @Override
    public String toString() {
      if (isNil()) {
        return "nil";
      } else {
        String color = isBlack ? "black" : "red";
        return "start = " + start() + "\nend = " + end() + "\nmaxEnd = " + maxEnd + "\ncolor = " + color;
      }
    }

    /**
     * Ensures that red-black constraints and interval-tree constraints are
     * maintained after an insertion.
     */
    private void insertFixup() {
      Node z = this;
      while (z.parent.isRed()) {
        if (z.parent.isLeftChild()) {
          Node y = z.parent.parent.right;
          if (y.isRed()) {
            z.parent.blacken();
            y.blacken();
            z.grandparent().redden();
            z = z.grandparent();
          } else {
            if (z.isRightChild()) {
              z = z.parent;
              z.leftRotate();
            }
            z.parent.blacken();
            z.grandparent().redden();
            z.grandparent().rightRotate();
          }
        } else {
          Node y = z.grandparent().left;
          if (y.isRed()) {
            z.parent.blacken();
            y.blacken();
            z.grandparent().redden();
            z = z.grandparent();
          } else {
            if (z.isLeftChild()) {
              z = z.parent;
              z.rightRotate();
            }
            z.parent.blacken();
            z.grandparent().redden();
            z.grandparent().leftRotate();
          }
        }
      }
      root.blacken();
    }

    ///////////////////////////////
    // Node -- Debugging methods //
    ///////////////////////////////

    /**
     * Whether or not the subtree rooted at this Node is a valid binary-search
     * tree.
     * 
     * @param min - a lower-bound Node
     * @param max - an upper-bound Node
     */
    private boolean isBST(Node min, Node max) {
      if (isNil()) {
        return true; // Leaves are a valid BST, trivially.
      }
      if (min != null && compareTo(min) <= 0) {
        return false; // This Node must be greater than min
      }
      if (max != null && compareTo(max) >= 0) {
        return false; // and less than max.
      }

      // Children recursively call method with updated min/max.
      return left.isBST(min, this) && right.isBST(this, max);
    }

    /**
     * Whether or not the subtree rooted at this Node is balanced.
     * <p>
     * Balance determination is done by calculating the black-height.
     * 
     * @param black - the expected black-height of this subtree
     */
    private boolean isBalanced(int black) {
      if (isNil()) {
        return black == 0; // Leaves have a black-height of zero,
      } // even though they are black.
      if (isBlack) {
        black--;
      }
      return left.isBalanced(black) && right.isBalanced(black);
    }

    /**
     * Whether or not the subtree rooted at this Node has a valid red-coloring.
     * <p>
     * A red-black tree has a valid red-coloring if every red node has two black
     * children.
     */
    private boolean hasValidRedColoring() {
      if (isNil()) {
        return true;
      } else if (isBlack) {
        return left.hasValidRedColoring() && right.hasValidRedColoring();
      } else {
        return left.isBlack && right.isBlack && left.hasValidRedColoring() && right.hasValidRedColoring();
      }
    }

    /**
     * Whether or not the subtree rooted at this Node has consistent maxEnd
     * values.
     * <p>
     * The maxEnd value of an interval-tree Node is equal to the maximum of the
     * end-values of all intervals contained in the Node's subtree.
     */
    private boolean hasConsistentMaxEnds() {

      if (isNil()) { // 1. sentinel node
        return true;
      }

      if (hasNoChildren()) { // 2. leaf node
        return maxEnd == end();
      } else {
        boolean consistent = maxEnd >= end();
        if (hasTwoChildren()) { // 3. two children
          return consistent && maxEnd >= left.maxEnd && maxEnd >= right.maxEnd && left.hasConsistentMaxEnds()
              && right.hasConsistentMaxEnds();
        } else if (left.isNil()) { // 4. one child -- right
          return consistent && maxEnd >= right.maxEnd && right.hasConsistentMaxEnds();
        } else {
          return consistent && // 5. one child -- left
              maxEnd >= left.maxEnd && left.hasConsistentMaxEnds();
        }
      }
    }
  }

  ///////////////////////
  // Tree -- Iterators //
  ///////////////////////

  /**
   * An Iterator which walks along this IntervalTree's Nodes in ascending order.
   */
  private class TreeNodeIterator implements Iterator<Node> {

    private Node next;

    private TreeNodeIterator(Node root) {
      next = root.minimumNode();
    }

    @Override
    public boolean hasNext() {
      return !next.isNil();
    }

    @Override
    public Node next() {
      if (!hasNext()) {
        throw new NoSuchElementException("Interval tree has no more elements.");
      }
      Node rtrn = next;
      next = rtrn.successor();
      return rtrn;
    }
  }

  /**
   * An Iterator which walks along this IntervalTree's Intervals in ascending
   * order.
   * <p>
   * This class just wraps a TreeNodeIterator and extracts each Node's Interval.
   */
  private class TreeIterator implements Iterator<T> {

    private TreeNodeIterator nodeIter;

    private TreeIterator(Node root) {
      nodeIter = new TreeNodeIterator(root);
    }

    @Override
    public boolean hasNext() {
      return nodeIter.hasNext();
    }

    @Override
    public T next() {
      return nodeIter.next().interval;
    }
  }

  ///////////////////////////////
  // Tree -- Debugging methods //
  ///////////////////////////////

  /**
   * Whether or not this IntervalTree is a valid binary-search tree.
   * <p>
   * This method will return false if any Node is less than its left child or
   * greater than its right child.
   * <p>
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @SuppressWarnings("unused")
  private boolean isBST() {
    return root.isBST(null, null);
  }

  /**
   * Whether or not this IntervalTree is balanced.
   * <p>
   * This method will return false if all of the branches (from root to leaf) do
   * not contain the same number of black nodes. (Specifically, the black-number
   * of each branch is compared against the black-number of the left-most
   * branch.)
   * <p>
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @SuppressWarnings("unused")
  private boolean isBalanced() {
    int black = 0;
    Node x = root;
    while (!x.isNil()) {
      if (x.isBlack) {
        black++;
      }
      x = x.left;
    }
    return root.isBalanced(black);
  }

  /**
   * Whether or not this IntervalTree has a valid red coloring.
   * <p>
   * This method will return false if all of the branches (from root to leaf) do
   * not contain the same number of black nodes. (Specifically, the black-number
   * of each branch is compared against the black-number of the left-most
   * branch.)
   * <p>
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @SuppressWarnings("unused")
  private boolean hasValidRedColoring() {
    return root.hasValidRedColoring();
  }

  /**
   * Whether or not this IntervalTree has consistent maxEnd values.
   * <p>
   * This method will only return true if each Node has a maxEnd value equal to
   * the highest interval end value of all the intervals in its subtree.
   * <p>
   * This method is used for debugging only, and its access is changed in
   * testing.
   */
  @SuppressWarnings("unused")
  private boolean hasConsistentMaxEnds() {
    return root.hasConsistentMaxEnds();
  }
}
