public class A {
    public static void main(String[] args) {
        new B().hello();
    }
    public void callB() {
        new B().hello();
    }
}
