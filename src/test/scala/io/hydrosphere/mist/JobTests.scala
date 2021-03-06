package  io.hydrosphere.mist

import io.hydrosphere.mist.contexts.ContextBuilder
import io.hydrosphere.mist.jobs._
import io.hydrosphere.mist.jobs.runners.Runner
import io.hydrosphere.mist.master._
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._
import spray.json.{DefaultJsonProtocol, pimpString}
import org.scalatest._

class JobRepositoryTest extends FunSuite with Eventually with BeforeAndAfterAll with JsonFormatSupport with DefaultJsonProtocol {

  val jobConfiguration = new FullJobConfiguration("", "", "Test Jobconfiguration")

  override def beforeAll(): Unit = {
    Thread.sleep(5000)
  }

  //InMapDbJobConfigurationRepository
  test("size InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    InMapDbJobConfigurationRepository.add("1", jobConfiguration)
    InMapDbJobConfigurationRepository.add("2", jobConfiguration)
    InMapDbJobConfigurationRepository.add("3", jobConfiguration)
    assert(InMapDbJobConfigurationRepository.size == 3)
  }

  test("Clear InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    assert(InMapDbJobConfigurationRepository.size == 0)
  }

  test("Add in InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    InMapDbJobConfigurationRepository.add("1", jobConfiguration)
    assert(InMapDbJobConfigurationRepository.size == 1)
  }

  test("Remove from InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    InMapDbJobConfigurationRepository.add("1", jobConfiguration)
    InMapDbJobConfigurationRepository.remove("1")
    assert(InMapDbJobConfigurationRepository.size == 0)
  }

  test("get from InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    InMapDbJobConfigurationRepository.add("1", jobConfiguration)
    assert( InMapDbJobConfigurationRepository.get("1").namespace == jobConfiguration.namespace)
  }

  test("getAll from InMapDbJobConfigurationRepository") {
    InMapDbJobConfigurationRepository.clear()
    InMapDbJobConfigurationRepository.add("1", jobConfiguration)
    val _collection = InMapDbJobConfigurationRepository.getAll
    val getjobConfiguration = _collection.get("1")
    assert( getjobConfiguration.get.namespace == jobConfiguration.namespace
            && _collection.size == InMapDbJobConfigurationRepository.size )
  }

  //InMemoryJobConfigurationRepository
  test("size InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.add("1", jobConfiguration)
    InMemoryJobConfigurationRepository.add("2", jobConfiguration)
    InMemoryJobConfigurationRepository.add("3", jobConfiguration)
    assert(InMemoryJobConfigurationRepository.size == 3)
  }

  test("Clear InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    assert(InMemoryJobConfigurationRepository.size == 0)
  }

  test("Add in InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.add("1", jobConfiguration)
    assert(InMemoryJobConfigurationRepository.size == 1)
  }

  test("Remove from InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.add("1", jobConfiguration)
    InMemoryJobConfigurationRepository.remove("1")
    assert(InMemoryJobConfigurationRepository.size == 0)
  }

  test("get from InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.add("1", jobConfiguration)
    assert( InMemoryJobConfigurationRepository.get("1").namespace == jobConfiguration.namespace)
  }

  test("getAll from InMemoryJobConfigurationRepository") {
    InMemoryJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.add("1", jobConfiguration)
    val _collection = InMemoryJobConfigurationRepository.getAll
    val getjobConfiguration = _collection.get("1")
    assert( getjobConfiguration.get.namespace == jobConfiguration.namespace
      && _collection.size == InMemoryJobConfigurationRepository.size )
  }

  override def afterAll(): Unit ={
    InMapDbJobConfigurationRepository.clear()
    InMemoryJobConfigurationRepository.clear()
  }
}

class JobTests extends FunSuite with Eventually with BeforeAndAfterAll with JsonFormatSupport with DefaultJsonProtocol {


  val jobConfiguration_Empty = new FullJobConfiguration("", "", "Empty Test Jobconfiguration", Map().empty, Option("1"))
  val jobConfiguration_Python = new FullJobConfiguration("some.py", "", "Python Test Jobconfiguration", Map().empty, Option("2"))
  val jobConfiguration_Jar = new FullJobConfiguration("some.jar", "", "Jar Test Jobconfiguration", Map().empty, Option("3"))
  val contextWrapper = ContextBuilder.namedSparkContext("foo")

  val versionRegex = "(\\d+)\\.(\\d+).*".r
  val sparkVersion = util.Properties.propOrNone("sparkVersion").getOrElse("[1.5.2, )")

  val checkSparkSessionLogic = {
    sparkVersion match {
      case versionRegex(major, minor) if major.toInt > 1 => true
      case _ => false
    }
  }

  override def beforeAll(): Unit = {
    Thread.sleep(5000)
  }

  test("FileNotFoundException") {
      intercept[java.lang.Exception] {
        val json = TestConfig.requestBadPatch.parseJson
        val jobConfiguration = json.convertTo[FullJobConfiguration]
        Runner(jobConfiguration, contextWrapper)
      }
  }

  test("Jar job") {
    val json = TestConfig.requestJar.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Initialized)
    }
  }

  test("Jar job sql") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestSparkSql.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(20 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Initialized)
    }
  }

  test("Jar job Hive") {
    val json = if(checkSparkSessionLogic) {
      TestConfig.requestSparkSession.parseJson
    }
    else {
      TestConfig.requestSparkhive.parseJson
    }
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Initialized)
    }
  }

  test("Py job") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestPyspark.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Initialized)
    }
  }

  test("Py job sql") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestPysparkSql.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Initialized)
    }
  }

  test("Py job hive") {
    val json = if(checkSparkSessionLogic) {
      TestConfig.requestPysparkSession.parseJson
    }
    else {
      TestConfig.requestPysparkHive.parseJson
    }
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Initialized)
    }
  }

  test("Jar job run") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestJar.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    someJarJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Stopped)
    }
  }

  test("Jar job sql run ") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestSparkSql.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    someJarJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Stopped)
    }
  }

  test("Jar job hive run ") {
    val json = if(checkSparkSessionLogic) {
      TestConfig.requestSparkSession.parseJson
    }
    else {
      TestConfig.requestSparkhive.parseJson
    }
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    someJarJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Stopped)
    }
  }

  test("Py job run") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestPyspark.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    somePyJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Stopped)
    }
  }

  test("Py job sql run") {
    if(checkSparkSessionLogic)
      cancel("Can't run in Spark 2.0.0")
    val json = TestConfig.requestPysparkSql.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    somePyJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Stopped)
    }
  }

  test("Py job hive run ") {
    val json = if(checkSparkSessionLogic) {
      TestConfig.requestPysparkSession.parseJson
    }
    else {
      TestConfig.requestPysparkHive.parseJson
    }
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    somePyJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Stopped)
    }
  }

  test("Jar job testerror run") {
    val json = TestConfig.requestTesterror.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val someJarJob = Runner(jobConfiguration, contextWrapper)
    someJarJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(someJarJob.status == Runner.Status.Aborted)
    }
  }

  test("Py job error ") {
    val json = TestConfig.requestPyError.parseJson
    val jobConfiguration = json.convertTo[FullJobConfiguration]
    val somePyJob = Runner(jobConfiguration, contextWrapper)
    somePyJob.run()
    eventually(timeout(30 seconds), interval(500 milliseconds)) {
      assert(somePyJob.status == Runner.Status.Aborted)
    }
  }


  override def afterAll(): Unit ={
    contextWrapper.stop()

    Thread.sleep(5000)
  }

}

