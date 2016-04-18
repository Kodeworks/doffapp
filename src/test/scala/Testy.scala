import java.util.NoSuchElementException

import com.kodeworks.doffapp.IdGen
import org.junit.{Assert, Test}
import shapeless._

import reflect.runtime.universe._

class Person

class Booking

class Charge

class MyDao(implicit val ltag: TypeTag[Person :: Booking :: HNil]) extends IdGen[Person :: Booking :: HNil]

class Testy {


  val myDao = new MyDao

  @Test(expected = classOf[NoSuchElementException])
  def test {
    Assert.assertEquals(0L, myDao.id[Person])
    Assert.assertEquals(1L, myDao.id[Person])
    Assert.assertEquals(0L, myDao.id[Booking])
    myDao.id[Charge]

  }
}