class A extends Throwable {
  final override def fillInStackTrace(): Throwable = this
}
