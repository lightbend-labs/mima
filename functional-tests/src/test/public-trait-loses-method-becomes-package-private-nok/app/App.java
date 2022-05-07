public final class App {
  public static void main(String[] args) {
    System.out.println((new bar.A() {}).<App>foo(new App()));
  }
}
