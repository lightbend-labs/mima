
trait Deployment {
  def foo: String = "abc"

  def bar(x: Int): Object = new Object

  def baz: String
}

class DeploymentStrategy extends Deployment {
  def baz = "abc"
  
  def deploy {

    baz
    bar(10)
  }
}
