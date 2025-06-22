package tf.bug.hexstudio.iota

import scala.collection.immutable.SortedMap
import tf.bug.hexstudio.iota.hexcasting.{BooleanIotaType, NullIotaType}

object IotaTypeRegistry {

  val registry: SortedMap[String, IotaType] = SortedMap(
    "hexcasting:boolean" -> BooleanIotaType,
    "hexcasting:null" -> NullIotaType
  )

}
