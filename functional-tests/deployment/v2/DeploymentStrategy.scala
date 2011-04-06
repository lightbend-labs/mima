
trait Deployment {
  def foo: Object = "abc"

  def bar(x: Int): Object = new Object

  def baz: String

  def bazImproved: Object = bar(1)
}

class DeploymentStrategy extends Deployment {
  def baz = "abc"
  var lock = new Object

  def deploy(foo: Object) {

    baz
    bar(10)
  }
}
