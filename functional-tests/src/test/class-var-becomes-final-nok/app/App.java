public class App {
  public static void main(String[] args) {
    // IMO, with no way to fail this in Scala, this shouldn't emit a problem
    System.out.println(new A() { public int foo() { return 11; } }.foo() + 12);
  }
}
