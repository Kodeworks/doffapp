import java.util.NoSuchElementException

import com.kodeworks.doffapp.IdGen
import org.junit.{Assert, Test}
import shapeless._

import reflect.runtime.universe._

class Person

class Booking

class Charge

class MyDao(implicit val ltag: TypeTag[Person :: Booking :: HNil]) extends IdGen

class IdGenTest {

  @Test(expected = classOf[NoSuchElementException])
  def test {
    val myDao = new MyDao
    Assert.assertEquals(0L, myDao.id[Person])
    Assert.assertEquals(1L, myDao.id[Person])
    Assert.assertEquals(0L, myDao.id[Booking])
    myDao.id[Charge]
  }
}