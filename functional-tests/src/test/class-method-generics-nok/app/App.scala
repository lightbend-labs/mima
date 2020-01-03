object App {
  def main(args: Array[String]): Unit = {
    val api = new Api
    val cov2 = api.cov2[String]
    println(cov2)
    println(api.con1(App))

    val abi = new Abi {
      def cov1()    = App
      def cov2[T]() = null.asInstanceOf[T]

      def con1(x: Any)  = ()
      def con2[T](x: T) = ()
    }
    val cov1: Any = abi.cov1
    println(cov1)
    println(abi.con2[App.type](App))
  }
}
