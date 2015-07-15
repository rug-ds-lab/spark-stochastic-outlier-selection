package kafka.serializer

import org.scalatest._

/**
 * Created by Fokko on 24-6-15.
 */
class CoderTest extends FlatSpec with Matchers with BeforeAndAfter {

  "En and decoding the value " should "give the same result" in {

    val inputVector = Array(19.0, 25.0, 22.0)

    val enc = new ArrayDoubleEncoder
    val dec = new ArrayDoubleDecoder

    inputVector should be(enc.fromBytes(dec.toBytes(inputVector)))
  }
}
