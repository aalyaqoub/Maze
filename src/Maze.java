import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;



//Button key:
//- D: depth search
//- B: breadth search
//- up: move current cell up
//- down: move current cell down
//- right: move current cell right
//- left: move current cell left
//- r: reset
//- v: toggle visited

interface ICollection<T> {
  boolean isEmpty();

  T remove();

  boolean add(T data);
}

class Queue<T> implements ICollection<T> {
  ArrayList<T> queue;

  Queue() {
    this.queue = new ArrayList<T>();
  }

  @Override
  public boolean isEmpty() {
    return this.queue.isEmpty();
  }

  @Override
  public T remove() {
    return this.queue.remove(0);
  }

  @Override
  public boolean add(T data) {
    return this.queue.add(data);
  }

}

class Stack<T> implements ICollection<T> {
  ArrayList<T> stack;

  Stack() {
    this.stack = new ArrayList<T>();
  }

  @Override
  public boolean isEmpty() {
    return this.stack.isEmpty();
  }

  @Override
  public T remove() {
    return this.stack.remove(0);
  }

  @Override
  public boolean add(T data) {
    this.stack.add(0, data);
    return true;
  }

}

// to represent a Maze
class Maze extends World {
  ArrayList<ArrayList<Cell>> board;
  Random rand;
  ArrayList<Edge> edgesInTree;
  ICollection<Cell> worklist;
  Deque<Cell> alreadySeen = new Deque<Cell>();
  HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
  Cell current;
  boolean showVisted = true;


  // create generic maze with dimensions x and y
  Maze(int x, int y) {
    if (x <= 1 || y <= 1) {
      throw new IllegalArgumentException("Not a valid game");
    }
    this.edgesInTree = new ArrayList<Edge>();
    this.board = new ArrayList<ArrayList<Cell>>();
    this.rand = new Random();
    this.makeBoard(x, y);
    this.worklist = new Stack<Cell>();
    this.current = this.board.get(0).get(0);
  }

  // creates maze with a given random seed
  Maze(int x, int y, Random rand) {
    if (x <= 1 || y <= 1) {
      throw new IllegalArgumentException("Not a valid game");
    }
    this.edgesInTree = new ArrayList<Edge>();
    this.board = new ArrayList<ArrayList<Cell>>();
    this.rand = rand;
    this.makeBoard(x, y);
    this.worklist = new Stack<Cell>();
    this.current = this.board.get(0).get(0);
  }

  // creates a maze with an empty board
  Maze() {
    this.edgesInTree = new ArrayList<Edge>();
    this.board = new ArrayList<ArrayList<Cell>>();
    this.rand = new Random(1);
    this.worklist = new Stack<Cell>();
  }

  //makes and connects the board for the game
  void makeBoard(int x, int y) {
    Cell top;
    Cell left;
    PriorityQueue<Edge> worklist = new PriorityQueue<Edge>(new EdgeComparator());
    for (int i = 0; i < y; i++) {
      ArrayList<Cell> row = new ArrayList<Cell>();
      for (int j = 0; j < x; j++) {
        if (i == 0) {
          top = null;
        }
        else {  
          top = this.board.get(i - 1).get(j);
        }
        if (j == 0) {
          left = null;
        }
        else {
          left = row.get(j - 1);
        }
        row.add(new Cell(left, top, j, i));
      }
      this.board.add(row);
    }
    for (ArrayList<Cell> row : this.board) {
      for (Cell c : row) {
        if (c.bottom != null) {
          worklist.add(new Edge(c, c.bottom, rand));
        }
        if (c.right != null) {
          worklist.add(new Edge(c, c.right, rand));
        }
      }
    }
    this.board.get(0).get(0).current = true;
    this.edgesInTree = this.kruskals(board, worklist);
  }

  void getPath(HashMap<Cell, Cell> map, Cell last) {
    if (last != null) {
      last.pathed = true;
      this.getPath(map, map.get(last));
    }
  }

  boolean isValidMove(Cell c1, Cell c2) {
    if (c2 != null) {
      for (Edge e : this.edgesInTree) {
        if (e.from.equals(c1) && e.to.equals(c2) || e.from.equals(c2) && e.to.equals(c1)) {
          return true;
        }
      }
    }
    return false;
  }

  //uses kruskal algorithms to get the single spanning tree
  ArrayList<Edge> kruskals(ArrayList<ArrayList<Cell>> board, PriorityQueue<Edge> worklist) {
    HashMap<Cell, Cell> map = new HashMap<Cell, Cell>();
    for (ArrayList<Cell> row : board) {
      for (Cell c : row) {
        map.put(c, c);
      }
    }
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    while (edgesInTree.size() < board.get(0).size() * board.size() - 1) {
      Edge curr = worklist.remove();
      if (this.getRepresentative(map, curr.from) != (this.getRepresentative(map, curr.to))) {
        edgesInTree.add(curr);
        map.replace(this.getRepresentative(map, curr.from), (this.getRepresentative(map, curr.to)));
      }
    }
    return edgesInTree;
  }

  // gets the representative of a cell in a hashmap
  Cell getRepresentative(HashMap<Cell, Cell> map, Cell c) {
    while (map.get(c) != c) {
      c = map.get(c);
    }
    return c;
  }

  //In Graph
  ArrayList<Cell> getConnections(Cell from, ArrayList<Edge> edgesInGraph) {
    ArrayList<Cell> edges = new ArrayList<Cell>();
    for (Edge e : edgesInGraph) {
      if (e.from.equals(from)) {
        edges.add(e.to);
      }
      if (e.to.equals(from)) {
        edges.add(e.from);
      }
    }
    return edges;
  }

  //draws a row of cells
  WorldImage drawRow(ArrayList<Cell> row) {
    WorldImage curr = new EmptyImage();
    for (Cell cell : row) {
      curr = new BesideImage(curr, cell.draw(this.showVisted));
    }
    return curr;
  }

  // draws the world for the game
  public WorldScene makeScene() {
    int x = this.board.get(0).size() * 20;
    int y = this.board.size() * 20;
    WorldImage curr = new EmptyImage();
    for (ArrayList<Cell> rows : board) {
      curr = new AboveImage(curr, this.drawRow(rows));
    }
    WorldScene img = new WorldScene(x, y);
    img.placeImageXY(curr, x / 2, y / 2);
    for (Edge e : this.edgesInTree) {
      WorldImage line = new LineImage(new Posn((e.to.y - e.from.y) * 18, 
          (e.to.x - e.from.x) * 18), Color.white).movePinhole(
              (e.to.y - e.from.y) * -10,  (e.to.x - e.from.x) * -10);
      img.placeImageXY(line, e.to.x * 20,  e.to.y * 20);
    }

    if (this.current.equals(this.board.get(this.board.size() - 1).get(
        this.board.get(0).size() - 1))) {
      WorldImage winText = new TextImage("Maze Complete!", 20, Color.black);
      img.placeImageXY(winText, x / 2, y / 2);
    }
    return img;
  }

  public void onKeyEvent(String key) {
    if (key.equalsIgnoreCase("d")) {
      for (ArrayList<Cell> row : this.board) {
        for (Cell c : row) {
          c.pathed = false;
          c.visited = false;
        }
      }
      this.current.current = false;
      this.current = this.board.get(0).get(0);
      worklist = new Stack<Cell>();
      worklist.add(this.board.get(0).get(0));
      alreadySeen = new Deque<Cell>();
    }
    if (key.equalsIgnoreCase("b")) {
      for (ArrayList<Cell> row : this.board) {
        for (Cell c : row) {
          c.pathed = false;
          c.visited = false;
        }
      }
      this.current.current = false;
      this.current = this.board.get(0).get(0);
      worklist = new Queue<Cell>();
      worklist.add(this.board.get(0).get(0));
      alreadySeen = new Deque<Cell>();
    }

    if (key.equalsIgnoreCase("left")) {
      if (this.isValidMove(this.current, this.current.left)) {
        if (!current.left.visited) {
          this.cameFromEdge.put(current.left, current);
        }
        this.current.visited = true;
        this.current.current = false;
        this.current = this.current.left;
        this.current.current = true;
      }
    }
    if (key.equalsIgnoreCase("right")) {
      if (this.isValidMove(this.current, this.current.right)) {
        if (!current.right.visited) {
          this.cameFromEdge.put(current.right, current);
        }
        this.current.visited = true;
        this.current.current = false;
        this.current = this.current.right;
        this.current.current = true;
      }
    }
    if (key.equalsIgnoreCase("up")) {
      if (this.isValidMove(this.current, this.current.top)) {
        if (!current.top.visited) {
          this.cameFromEdge.put(current.top, current);
        }
        this.current.visited = true;
        this.current.current = false;
        this.current = this.current.top;
        this.current.current = true;
      }
    }
    if (key.equalsIgnoreCase("down")) {
      if (this.isValidMove(this.current, this.current.bottom)) {
        if (!current.bottom.visited) {
          this.cameFromEdge.put(current.bottom, current);
        }
        this.current.visited = true;
        this.current.current = false;
        this.current = this.current.bottom;
        this.current.current = true;
      }
    }
    if (key.equalsIgnoreCase("v")) {
      this.showVisted = !this.showVisted;
    }

    if (key.equalsIgnoreCase("r")) {
      this.edgesInTree = new ArrayList<Edge>();
      this.rand = new Random();
      int x = this.board.get(0).size();
      int y = this.board.size();
      this.board = new ArrayList<ArrayList<Cell>>();
      this.makeBoard(x, y);
      this.worklist = new Stack<Cell>();
      this.current = this.board.get(0).get(0);
    }
  }

  public void onTick() {
    if (this.current.equals(this.board.get(this.board.size() - 1).get(
        this.board.get(0).size() - 1))) {
      this.getPath(this.cameFromEdge, this.current);
    }
    if (!worklist.isEmpty()) {
      Cell next = worklist.remove();
      next.visited = true;
      if (next.equals(this.board.get(this.board.size() - 1).get(
          this.board.get(0).size() - 1))) {
        worklist = new Queue<Cell>();
        this.getPath(cameFromEdge, next);
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        // add all the neighbors of next to the worklist for further processing
        for (Cell c : this.getConnections(next, this.edgesInTree)) {
          worklist.add(c);
          if (!alreadySeen.contains(c)) {
            this.cameFromEdge.put(c, next);
          }
        }
        // add next to alreadySeen, since we're done with it
        alreadySeen.addAtHead(next);
      }
    }
  }

}

// represents a cell in the game
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;

  final int SIZE = 20;
  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;
  boolean visited;
  boolean pathed;
  boolean current;

  // Constructor
  Cell(Cell left, Cell top, int x, int y) {
    this.left = left;
    this.top = top;
    // the way the board is made, the current cell will not yet have a right and bottom cell
    this.right = null;
    this.bottom = null;

    if (this.left != null) {
      this.left.right = this;
    }

    if (this.top != null) {
      this.top.bottom = this;
    }

    this.x = x;
    this.y = y;
    this.visited = false;
    this.pathed = false;
    this.current = false;
  }

  // Draws a single cell
  WorldImage draw(boolean showVisited) {
    if (this.pathed) {
      return new OverlayImage(new RectangleImage(SIZE, SIZE, "outline", Color.gray),
          new RectangleImage(SIZE - 10, SIZE - 10, "solid", Color.cyan));
    }
    else if (this.current) {
      return new OverlayImage(new RectangleImage(SIZE, SIZE, "outline", Color.gray),
          new RectangleImage(SIZE - 10, SIZE - 10, "solid", Color.green));
    } 
    else if (this.visited && showVisited) {
      return new OverlayImage(new RectangleImage(SIZE, SIZE, "outline", Color.gray),
          new RectangleImage(SIZE - 10, SIZE - 10, "solid", Color.red));
    }
    return new RectangleImage(SIZE, SIZE, "outline", Color.gray);
  }
}

// represents an edge connecting two cells
class Edge {
  Cell from;
  Cell to;
  int weight;

  // makes an edge given a weight
  Edge(Cell from, Cell to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // makes an edge with a given random
  Edge(Cell from, Cell to, Random rand) {
    this.from = from;
    this.to = to;
    this.weight = rand.nextInt(10);
  }
}

//Represents a Deque
class Deque<T> {
  Sentinel<T> header;

  // Constructors
  Deque() {
    this.header = new Sentinel<T>();
  }

  public boolean contains(T data) {
    return this.header.next.containsHelp(data);
  }

  Deque(Sentinel<T> header) {
    this.header = header;
  }

  // returns the size of the deque
  int size() {
    return this.header.next.sizeHelp();
  }

  // adds a node to the Deque with a given previous node and data
  void add(ANode<T> prev, T data) {
    new Node<T>(data, prev.next, prev);
  }

  // adds a node at the head of the deque with given data
  void addAtHead(T data) {
    this.add(this.header, data);
  }

  // adds a node at the tail of the deque with given data
  void addAtTail(T data) {
    this.add(this.header.prev, data);
  }

  // removes a node from the head of the deque
  T removeFromHead() {
    return this.header.next.remove();
  }

  // removes a node from the tail of the deque
  T removeFromTail() {
    return this.header.prev.remove();
  }

  // removes a given node from the deque
  void removeNode(ANode<T> node) {
    node.remove();
  }
}

//represents an abstract Node
abstract class ANode<T> {
  ANode<T> next;
  ANode<T> prev;

  // computes the size of the Deque
  abstract int sizeHelp();

  abstract boolean containsHelp(T data);

  // removes a node from the Deque
  abstract T remove();
}

//represents a sentinel node in the Deque
class Sentinel<T> extends ANode<T> {

  // Constructor
  Sentinel() {
    this.next = this;
    this.prev = this;
  }

  // computes the size of the Deque
  int sizeHelp() {
    return 0;
  }

  // removes a node from the Deque
  T remove() {
    throw new RuntimeException("Can't remove from empty list");
  }


  boolean containsHelp(T data) {
    return false;
  }

}

//represents a Node in a Deque
class Node<T> extends ANode<T> {
  T data;

  // Constructors
  Node(T data) {
    this.data = data;
    this.next = null;
    this.prev = null;
  }

  Node(T data, ANode<T> next, ANode<T> prev) {
    this.data = data;
    if (next == null || prev == null) {
      throw new IllegalArgumentException("Can't assign null");
    }
    this.next = next;
    this.prev = prev;
    next.prev = this;
    prev.next = this;
  }

  // computes the size of the Deque
  int sizeHelp() {
    return 1 + this.next.sizeHelp();
  }

  // removes a node from the Deque
  T remove() {
    this.prev.next = this.next;
    this.next.prev = this.prev;
    return this.data;
  }

  @Override
  boolean containsHelp(T data) {
    if (this.data.equals(data)) {
      return true;
    }
    return this.next.containsHelp(data);
  }
}

// compares two edges by their weight
class EdgeComparator implements Comparator<Edge> {

  // compares two edges by weight, returns the difference in weight
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

//Maze examples
class ExamplesMaze {
  Random rand;
  ArrayList<Edge> edgesInTree;
  Maze maze;
  Maze maze1;
  Maze maze2;
  Cell cell00;
  Cell cell10;
  Cell cell01;
  Cell cell11;
  ArrayList<Cell> row0;
  ArrayList<Cell> row1;
  ArrayList<ArrayList<Cell>> board;
  Cell cellTwo00;
  Cell cellTwo10;
  Cell cellTwo20;
  Cell cellTwo01;
  Cell cellTwo11;
  Cell cellTwo21;
  Cell cellTwo02;
  Cell cellTwo12;
  Cell cellTwo22;
  ArrayList<Cell> rowTwo0;
  ArrayList<Cell> rowTwo1;
  ArrayList<Cell> rowTwo2;
  ArrayList<ArrayList<Cell>> boardTwo;
  ArrayList<Edge> edges;
  HashMap<Cell, Cell> map;
  PriorityQueue<Edge> worklist;
  Queue<Cell> mtq;
  Queue<Cell> q;
  Stack<Cell> mts;
  Stack<Cell> s;
  Maze maze3;
  Deque<String> deque1 = new Deque<String>();

  Sentinel<String> sentinel1;
  ANode<String> abc;
  ANode<String> bcd;
  ANode<String> cde;
  ANode<String> def;
  Deque<String> deque2;

  Sentinel<String> sentinel2;
  ANode<String> dog;
  ANode<String> cat;
  ANode<String> rabbit;
  ANode<String> alpaca;
  Deque<String> deque3;

  Sentinel<String> sentinel3;

  // initializes mazes and lists needed for testing
  void initData() {
    this.rand = new Random(1);

    this.maze = new Maze(3,3, this.rand);
    this.maze1 = new Maze();
    this.maze2 = new Maze(2,2, this.rand);
    this.maze3 = new Maze(4,4, this.rand);

    this.cell00 = new Cell(null, null,0, 0);
    this.cell10 = new Cell(cell00, null, 1, 0);
    this.cell01 = new Cell(null, cell00, 0, 1);
    this.cell11 = new Cell(cell01, cell10, 1, 1);
    this.cell00.current = true;

    this.row0 = new ArrayList<Cell>();
    this.row0.add(cell00);
    this.row0.add(cell10);

    this.row1 = new ArrayList<Cell>();
    this.row1.add(cell01);
    this.row1.add(cell11);

    this.board = new ArrayList<ArrayList<Cell>>();
    this.board.add(row0);
    this.board.add(row1);

    this.cellTwo00 = new Cell(null, null,0, 0);
    this.cellTwo10 = new Cell(cellTwo00, null, 1, 0);
    this.cellTwo20 = new Cell(cellTwo10, null, 2, 0);
    this.cellTwo01 = new Cell(null, cellTwo00, 0, 1);
    this.cellTwo11 = new Cell(cellTwo01, cellTwo10, 1, 1);
    this.cellTwo21 = new Cell(cellTwo11, cellTwo20, 2, 1);
    this.cellTwo02 = new Cell(null, cellTwo01, 0, 2);
    this.cellTwo12 = new Cell(cellTwo02, cellTwo11, 1, 2);
    this.cellTwo22 = new Cell(cellTwo12, cellTwo21, 2, 2);
    this.cellTwo00.current = true;

    this.rowTwo0 = new ArrayList<Cell>();
    this.rowTwo0.add(cellTwo00);
    this.rowTwo0.add(cellTwo10);
    this.rowTwo0.add(cellTwo20);

    this.rowTwo1 = new ArrayList<Cell>();
    this.rowTwo1.add(cellTwo01);
    this.rowTwo1.add(cellTwo11);
    this.rowTwo1.add(cellTwo21);

    this.rowTwo2 = new ArrayList<Cell>();
    this.rowTwo2.add(cellTwo02);
    this.rowTwo2.add(cellTwo12);
    this.rowTwo2.add(cellTwo22);

    this.boardTwo = new ArrayList<ArrayList<Cell>>();
    this.boardTwo.add(rowTwo0);
    this.boardTwo.add(rowTwo1);
    this.boardTwo.add(rowTwo2);

    this.worklist = new PriorityQueue<Edge>(new EdgeComparator());
    for (ArrayList<Cell> row : this.maze.board) {
      for (Cell c : row) {
        if (c.bottom != null) {
          this.worklist.add(new Edge(c, c.bottom, rand));
        }
        if (c.right != null) {
          this.worklist.add(new Edge(c, c.right, rand));
        }
      }
    }
    this.edges = new ArrayList<Edge>();
    this.edges.add(new Edge(this.cellTwo00, this.cellTwo10, 0));
    this.edges.add(new Edge(this.cellTwo11, this.cellTwo21, 0));
    this.edges.add(new Edge(this.cellTwo01, this.cellTwo11, 0));
    this.edges.add(new Edge(this.cellTwo02, this.cellTwo12, 2));
    this.edges.add(new Edge(this.cellTwo01, this.cellTwo02, 2));
    this.edges.add(new Edge(this.cellTwo12, this.cellTwo22, 3));
    this.edges.add(new Edge(this.cellTwo00, this.cellTwo01, 5));
    this.edges.add(new Edge(this.cellTwo20, this.cellTwo21, 7));

    this.map = new HashMap<Cell,Cell>();
    for (ArrayList<Cell> row : this.boardTwo) {
      for (Cell c : row) {
        this.map.put(c, c);
      }
    }
    this.mtq = new Queue<Cell>();
    this.q = new Queue<Cell>();
    this.mts = new Stack<Cell>();
    this.s = new Stack<Cell>();
    q.queue = row0;
    s.stack = row0;

    deque1 = new Deque<String>();

    sentinel1 = new Sentinel<String>();
    abc = new Node<String>("abc", this.sentinel1, this.sentinel1);
    bcd = new Node<String>("bcd", this.sentinel1, this.abc);
    cde = new Node<String>("cde", this.sentinel1, this.bcd);
    def = new Node<String>("def", this.sentinel1, this.cde);
    deque2 = new Deque<String>(this.sentinel1);

    sentinel2 = new Sentinel<String>();
    dog = new Node<String>("dog", this.sentinel2, this.sentinel2);
    cat = new Node<String>("cat", this.sentinel2, this.dog);
    rabbit = new Node<String>("rabbit", this.sentinel2, this.cat);
    alpaca = new Node<String>("alpaca", this.sentinel2, this.rabbit);
    deque3 = new Deque<String>(this.sentinel2);

    sentinel3 = new Sentinel<String>();
  }

  // tests Kruskals' algorithm
  boolean testKruskals(Tester t) {
    initData();
    ArrayList<Edge> edgeTest = this.maze.kruskals(this.maze.board, worklist);
    return t.checkExpect(this.edges, edgeTest);
  }

  // test getRepresentative
  void testGetRepresentative(Tester t) {
    initData();
    for (ArrayList<Cell> row : this.boardTwo) {
      for (Cell c : row) {
        t.checkExpect(this.maze.getRepresentative(this.map, c), c);
      }
    }
    this.map.replace(this.maze.getRepresentative(this.map, this.cellTwo00),
        (this.maze.getRepresentative(map, this.cellTwo01)));

    t.checkExpect(this.maze.getRepresentative(this.map, this.cellTwo00), this.cellTwo01);

    this.map.replace(this.maze.getRepresentative(this.map, this.cellTwo01),
        (this.maze.getRepresentative(map, this.cellTwo02)));
    t.checkExpect(this.maze.getRepresentative(this.map, this.cellTwo00), this.cellTwo02);
    t.checkExpect(this.maze.getRepresentative(this.map, this.cellTwo01), this.cellTwo02);
  }

  // test isEmpty() (Queue)
  void testIsEmpty(Tester t) {
    initData();
    t.checkExpect(mtq.isEmpty(), true);
    t.checkExpect(q.isEmpty(), false);
  }

  // test remove() (Queue)
  void testRemoveQ(Tester t) {
    initData();
    t.checkException(new IndexOutOfBoundsException("Index 0 out of bounds for length 0"),
        new Queue<Cell>(), "remove");
    t.checkExpect(q.queue.size(), 2);
    q.remove();
    t.checkExpect(q.queue.size(), 1);
  }

  // test add() (Queue)
  void testAddQ(Tester t) {
    initData();
    t.checkExpect(mtq.queue.size(), 0);
    mtq.add(cellTwo00);
    t.checkExpect(mtq.queue.size(), 1);
    t.checkExpect(q.queue.size(), 2);
    q.add(cellTwo00);
    t.checkExpect(q.queue.size(), 3);
  }

  // test isEmpty() (Stack)
  void testIsEmptyS(Tester t) {
    initData();
    t.checkExpect(mts.isEmpty(), true);
    t.checkExpect(s.isEmpty(), false);
  }

  // test remove() (Stack)
  void testRemoveS(Tester t) {
    initData();
    t.checkException(new IndexOutOfBoundsException("Index 0 out of bounds for length 0"),
        new Stack<Cell>(), "remove");
    t.checkExpect(s.stack.size(), 2);
    s.remove();
    t.checkExpect(s.stack.size(), 1);
  }

  // test add() (Stack)
  void testAddS(Tester t) {
    initData();
    t.checkExpect(mts.stack.size(), 0);
    mts.add(cellTwo00);
    t.checkExpect(mts.stack.size(), 1);
    t.checkExpect(s.stack.size(), 2);
    s.add(cellTwo00);
    t.checkExpect(s.stack.size(), 3);
  }

  //test makeBoard()
  void testMakeBoard(Tester t) {
    initData();
    t.checkExpect(this.maze1.board, new ArrayList<ArrayList<Cell>>());
    this.maze1.makeBoard(2,2);
    t.checkExpect(this.maze1.board, this.board);
    initData();
    t.checkExpect(this.maze1.board, new ArrayList<ArrayList<Cell>>());
    this.maze1.makeBoard(3, 3);
    t.checkExpect(this.maze1.board, this.boardTwo);
  }

  // test getPath()
  void testGetPath(Tester t) {
    initData();
    this.maze.getPath(this.map, this.cell00);
    t.checkExpect(this.cell00.pathed, true);
    this.maze.getPath(this.map, this.cell01);
    t.checkExpect(this.cell01.pathed, true);
    this.maze.getPath(this.map, this.cell11);
    t.checkExpect(this.cell11.pathed, true);
  }

  // test isValidMove()
  void testIsValidMove(Tester t) {
    initData();
    t.checkExpect(this.maze2.isValidMove(this.maze2.current, this.maze2.current.right), true);
    t.checkExpect(this.maze2.isValidMove(this.maze2.current, this.maze2.current.bottom), false);
    t.checkExpect(this.maze2.isValidMove(this.maze2.current, this.maze2.current.left), false);
    t.checkExpect(this.maze3.isValidMove(this.maze3.current, this.maze3.current.right), true);
    t.checkExpect(this.maze3.isValidMove(this.maze3.current, this.maze3.current.bottom), true);
    t.checkExpect(this.maze3.isValidMove(this.maze3.current, this.maze3.current.left), false);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.right), false);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.bottom), true);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.left), false);
    // moves the point so that its possible to test the top and left
    this.maze.onKeyEvent("down");
    this.maze.onKeyEvent("right");
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.right), false);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.left), true);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.bottom), true);
    t.checkExpect(this.maze.isValidMove(this.maze.current, this.maze.current.top), true);
  }

  //test drawRow()
  void testDrawRow(Tester t) {
    initData();
    WorldImage image = new EmptyImage();
    image = new BesideImage(image, this.cell00.draw(true));
    image = new BesideImage(image, this.cell10.draw(true));
    t.checkExpect(this.maze1.drawRow(this.row0), image);
    image = new BesideImage(image, this.cellTwo21.draw(true));
    t.checkExpect(this.maze.drawRow(this.rowTwo0), image);
  }

  //test makeScene()
  void testMakeScene(Tester t) {
    initData();
    WorldImage image = new EmptyImage();
    image = new AboveImage(image, this.maze.drawRow(rowTwo0));
    image = new AboveImage(image, this.maze.drawRow(rowTwo1));
    image = new AboveImage(image, this.maze.drawRow(rowTwo2));
    WorldScene scene = new WorldScene(60, 60);
    scene.placeImageXY(image, 30, 30);
    WorldImage line1 = new LineImage(new Posn(0, 18), Color.white).movePinhole(0, -10);
    WorldImage line2 = new LineImage(new Posn(18, 0), Color.white).movePinhole(-10, 0);
    scene.placeImageXY(line1, 40, 0);
    scene.placeImageXY(line1, 40, 40);
    scene.placeImageXY(line2, 40, 20);
    scene.placeImageXY(line2, 0, 40);
    scene.placeImageXY(line1, 20, 20);
    scene.placeImageXY(line2, 0, 20);
    scene.placeImageXY(line2, 20, 40);
    scene.placeImageXY(line2, 20, 20);
    t.checkExpect(this.maze.makeScene(), scene);
    WorldImage image2 = new EmptyImage();
    image2 = new AboveImage(image2, this.maze2.drawRow(row0));
    image2 = new AboveImage(image2, this.maze2.drawRow(row1));
    WorldScene scene2 = new WorldScene(40, 40);
    scene2.placeImageXY(image2, 20, 20);
    scene2.placeImageXY(line2, 20, 20);
    scene2.placeImageXY(line1, 20, 0);
    scene2.placeImageXY(line1, 20, 20);
    t.checkExpect(this.maze2.makeScene(), scene2);
  }

  //test onKeyEvent()
  void testOnKeyEvent(Tester t) {
    initData();
    // testing if you can't move to a particular cell
    t.checkExpect(this.maze.current, this.cellTwo00);
    this.maze.onKeyEvent("left");
    t.checkExpect(this.maze.current, this.cellTwo00);
    this.maze.onKeyEvent("right");
    t.checkExpect(this.maze.current, this.cellTwo00);
    this.maze.onKeyEvent("up");
    t.checkExpect(this.maze.current, this.cellTwo00);
    // test if moving updates visited and current
    this.maze.onKeyEvent("down");
    this.cellTwo00.current = false;
    this.cellTwo00.visited = true;
    this.cellTwo01.current = true;
    t.checkExpect(this.maze.current, this.cellTwo01);
    this.maze.onKeyEvent("right");
    this.cellTwo01.current = false;
    this.cellTwo01.visited = true;
    this.cellTwo11.current = true;
    t.checkExpect(this.maze.current, this.cellTwo11);
    this.maze.onKeyEvent("up");
    this.cellTwo11.current = false;
    this.cellTwo11.visited = true;
    this.cellTwo10.current = true;
    t.checkExpect(this.maze.current, this.cellTwo10);
    this.maze.onKeyEvent("down");
    this.cellTwo10.current = false;
    this.cellTwo10.visited = true;
    this.maze.onKeyEvent("down");
    this.cellTwo12.current = true;
    t.checkExpect(this.maze.current, this.cellTwo12);
    this.maze.onKeyEvent("down");
    t.checkExpect(this.maze.current, this.cellTwo12);
    this.maze.onKeyEvent("right");
    this.cellTwo12.current = false;
    this.cellTwo12.visited = true;
    this.cellTwo22.current = true;
    t.checkExpect(this.maze.current, this.cellTwo22);
    // testing if going backwards keeps track of visited
    this.maze.onKeyEvent("left");
    this.cellTwo22.current = false;
    this.cellTwo22.visited = true;
    this.cellTwo12.current = true;
    t.checkExpect(this.maze.current, this.cellTwo12);
    this.maze.onKeyEvent("up");
    this.cellTwo12.current = false;
    this.cellTwo11.current = true;
    t.checkExpect(this.maze.current, this.cellTwo11);
    this.maze.onKeyEvent("left");
    this.cellTwo11.current = false;
    this.cellTwo01.current = true;
    t.checkExpect(this.maze.current, this.cellTwo01);
    this.maze.onKeyEvent("right");
    this.cellTwo01.current = false;
    this.cellTwo11.current = true;
    t.checkExpect(this.maze.current, this.cellTwo11);
    this.maze.onKeyEvent("down");
    this.cellTwo11.current = false;
    this.cellTwo12.current = true;
    t.checkExpect(this.maze.current, this.cellTwo12);
    this.maze.onKeyEvent("r");
    t.checkExpect(!this.boardTwo.equals(this.maze.board), true);
    t.checkExpect(this.maze.current, this.maze.board.get(0).get(0));
  }

  //test onTick()
  void testOnTick(Tester t) {
    initData();
    t.checkExpect(this.boardTwo, this.maze.board);
    this.maze.onTick();
    t.checkExpect(this.boardTwo, this.maze.board);
    this.maze.onKeyEvent("d");
    this.maze.onTick();
    this.boardTwo.get(0).get(0).visited = true;
    this.boardTwo.get(0).get(0).current = false;
    this.boardTwo.get(1).get(0).current = false;
    t.checkExpect(this.boardTwo, this.maze.board);
  }

  //test draw() for Cell
  void testCellDraw(Tester t) {
    initData();
    this.cell10.pathed = true;
    this.cell11.visited = true;
    WorldImage currentCell = new OverlayImage(new RectangleImage(20, 20, "outline", Color.gray),
        new RectangleImage(10, 10, "solid", Color.green));
    WorldImage pathedCell = new OverlayImage(new RectangleImage(20, 20, "outline", Color.gray),
        new RectangleImage(10, 10, "solid", Color.cyan));
    WorldImage visitedCell = new OverlayImage(new RectangleImage(20, 20, "outline", Color.gray),
        new RectangleImage(10, 10, "solid", Color.red));
    t.checkExpect(this.cell00.draw(true), currentCell);
    t.checkExpect(this.cell10.draw(true), pathedCell);
    t.checkExpect(this.cell11.draw(true), visitedCell);
    t.checkExpect(this.cell11.draw(false), new RectangleImage(20, 20, "outline", Color.gray));
    t.checkExpect(this.cell01.draw(true), new RectangleImage(20, 20, "outline", Color.gray));
  }

  //test compare
  void testCompare(Tester t) {
    initData();
    Edge edge1 = this.maze2.edgesInTree.get(0);
    Edge edge2 = this.maze2.edgesInTree.get(1);
    t.checkExpect(new EdgeComparator().compare(edge1, edge2), -1);
    edge1 = this.maze.edgesInTree.get(0);
    edge2 = this.maze.edgesInTree.get(1);
    t.checkExpect(new EdgeComparator().compare(edge1, edge2), 0);
    edge1 = this.maze.edgesInTree.get(4);
    edge2 = this.maze.edgesInTree.get(5);
    t.checkExpect(new EdgeComparator().compare(edge2, edge1), 1);
  }

  // tests the constructor exceptions
  boolean testConstructor(Tester t) {
    return t.checkConstructorException(
        new IllegalArgumentException("Can't assign null"),
        "Node",
        "abc", null, null)
        && t.checkConstructorException(
            new IllegalArgumentException("Can't assign null"),
            "Node",
            "bcd", this.abc, null)
        && t.checkConstructorException(
            new IllegalArgumentException("Can't assign null"),
            "Node",
            "bcd", null, this.abc);
  }

  // checks Deque size method
  boolean testDequeSize(Tester t) {
    this.initData();
    return t.checkExpect(this.deque1.size(), 0)
        && t.checkExpect(this.deque2.size(), 4);
  }

  // tests add
  void testAdd(Tester t) {
    this.initData();
    t.checkExpect(this.deque1.size(), 0);
    this.deque1.add(this.deque1.header, "hi");
    t.checkExpect(this.deque1.size(), 1);
    t.checkExpect(this.deque1.header.next, 
        new Node<String>("hi", this.deque1.header, this.deque1.header));
    t.checkExpect(this.cat.next, this.rabbit);
    t.checkExpect(this.rabbit.prev, this.cat);
    t.checkExpect(this.deque3.size(), 4);
    this.deque3.add(this.cat, "cow");
    t.checkExpect(this.cat.next, new Node<String>("cow", this.rabbit, this.cat));
    t.checkExpect(this.rabbit.prev, new Node<String>("cow", this.rabbit, this.cat));
    t.checkExpect(this.deque3.size(), 5);
  }

  //test addAtHead
  void testAddAtHead(Tester t) {
    this.initData();
    t.checkExpect(this.deque1.header.next, this.sentinel3);
    t.checkExpect(this.deque1.size(), 0);
    this.deque1.addAtHead("Words");
    t.checkExpect(this.deque1.header.next, new Node<String>("Words", this.sentinel3, 
        this.sentinel3));
    t.checkExpect(this.deque1.size(), 1);
    t.checkExpect(this.deque2.header.next, this.abc);
    t.checkExpect(this.deque2.size(), 4);
    this.deque2.addAtHead("lol");
    t.checkExpect(this.deque2.header.next, new Node<String>("lol", this.abc, this.sentinel1));
    t.checkExpect(this.deque2.size(), 5);
  }

  //test addAtTail
  void testAddAtTail(Tester t) {
    this.initData();
    t.checkExpect(this.deque1.header.prev, this.sentinel3);
    t.checkExpect(this.deque1.size(), 0);
    this.deque1.addAtTail("bob");
    t.checkExpect(this.deque1.header.next, new Node<String>("bob", this.sentinel3, this.sentinel3));
    t.checkExpect(this.deque1.size(), 1);
    t.checkExpect(this.deque2.header.prev, this.def);
    t.checkExpect(this.deque2.size(), 4);
    this.deque2.addAtTail("ben");
    t.checkExpect(this.deque2.header.prev, new Node<String>("ben", this.sentinel1, this.def));
    t.checkExpect(this.deque2.size(), 5);
  }

  // tests removeFromHead method
  void testRemoveFromHead(Tester t) {
    this.initData();
    t.checkException(new RuntimeException("Can't remove from empty list"), this.deque1,
        "removeFromHead");
    t.checkExpect(this.sentinel2.next, this.dog);
    t.checkExpect(this.cat.prev, this.dog);
    t.checkExpect(this.deque3.size(), 4);
    this.deque3.removeFromHead();
    t.checkExpect(this.sentinel2.next, this.cat);
    t.checkExpect(this.cat.prev, this.sentinel2);
    t.checkExpect(this.deque3.size(), 3);
  }

  // tests removeFromTail method
  void testRemoveFromTail(Tester t) {
    this.initData();
    t.checkException(new RuntimeException("Can't remove from empty list"), this.deque1,
        "removeFromTail");
    t.checkExpect(this.sentinel2.prev, this.alpaca);
    t.checkExpect(this.rabbit.next, this.alpaca);
    t.checkExpect(this.deque3.size(), 4);
    this.deque3.removeFromTail();
    t.checkExpect(this.sentinel2.prev, this.rabbit);
    t.checkExpect(this.rabbit.next, this.sentinel2);
    t.checkExpect(this.deque3.size(), 3);
  }

  //test removeNode
  void testRemoveNode(Tester t) {
    this.initData();
    t.checkExpect(this.deque2.size(), 4);
    t.checkExpect(this.deque2.header.next, this.abc);
    this.deque2.removeNode(this.abc);
    t.checkExpect(this.deque2.size(), 3);
    t.checkExpect(this.deque2.header.next, this.bcd);
  }

  // tests sizeHelp method
  boolean testSizeHelp(Tester t) {
    this.initData();
    return t.checkExpect(this.sentinel1.sizeHelp(), 0)
        && t.checkExpect(this.abc.sizeHelp(), 4)
        && t.checkExpect(this.bcd.sizeHelp(), 3)
        && t.checkExpect(this.cde.sizeHelp(), 2)
        && t.checkExpect(this.def.sizeHelp(), 1);
  }

  // tests remove method
  void testRemove(Tester t) {
    this.initData();
    t.checkExpect(this.sentinel1.next, this.abc);
    t.checkExpect(this.bcd.prev, this.abc);
    t.checkExpect(this.deque2.size(), 4);
    this.abc.remove();
    t.checkExpect(this.sentinel1.next, this.bcd);
    t.checkExpect(this.bcd.prev, this.sentinel1);
    t.checkExpect(this.deque2.size(), 3)  ;
  }

  //tests bigbang
  void testBigBang(Tester t) {
    initData();
    Maze w = new Maze(15, 15);
    int worldWidth = w.board.get(0).size() * 20;
    int worldHeight = w.board.size() * 20;
    double tickRate = .000000000000000000001;
    w.bigBang(worldWidth, worldHeight, tickRate);
  }

}