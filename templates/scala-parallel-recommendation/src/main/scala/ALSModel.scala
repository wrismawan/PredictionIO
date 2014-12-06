package org.apache.spark.mllib.recommendation
// This must be the same package as Spark's MatrixFactorizationModel because
// MatrixFactorizationModel's constructor is private and we are using
// its constructor in order to save and load the model

import org.template.recommendation.ALSAlgorithmParams

import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

class ALSModel(
    override val rank: Int,
    override val userFeatures: RDD[(Int, Array[Double])],
    override val productFeatures: RDD[(Int, Array[Double])],
    val userIdToIxMap: BiMap[String, Int],
    val productIdToIxMap: BiMap[String, Int])
  extends MatrixFactorizationModel(rank, userFeatures, productFeatures)
  with IPersistentModel[ALSAlgorithmParams] {

  def save(id: String, params: ALSAlgorithmParams,
    sc: SparkContext): Boolean = {

    sc.parallelize(Seq(rank)).saveAsObjectFile(s"/tmp/${id}/rank")
    userFeatures.saveAsObjectFile(s"/tmp/${id}/userFeatures")
    productFeatures.saveAsObjectFile(s"/tmp/${id}/productFeatures")
    sc.parallelize(Seq(userIdToIxMap))
      .saveAsObjectFile(s"/tmp/${id}/userIdToIxMap")
    sc.parallelize(Seq(productIdToIxMap))
      .saveAsObjectFile(s"/tmp/${id}/productIdToIxMap")
    true
  }

  override def toString = {
    s"userFeatures: [${userFeatures.count()}]" +
    s"(${userFeatures.take(2).toList}...)" +
    s" productFeatures: [${productFeatures.count()}]" +
    s"(${productFeatures.take(2).toList}...)" +
    s" userIdToIxMap: [${userIdToIxMap.size}]" +
    s"(${userIdToIxMap.take(2)}...)" +
    s" productIdToIxMap: [${productIdToIxMap.size}]" +
    s"(${productIdToIxMap.take(2)}...)"
  }
}

object ALSModel
  extends IPersistentModelLoader[ALSAlgorithmParams, ALSModel] {
  def apply(id: String, params: ALSAlgorithmParams,
    sc: Option[SparkContext]) = {
    new ALSModel(
      rank = sc.get.objectFile[Int](s"/tmp/${id}/rank").first,
      userFeatures = sc.get.objectFile(s"/tmp/${id}/userFeatures"),
      productFeatures = sc.get.objectFile(s"/tmp/${id}/productFeatures"),
      userIdToIxMap = sc.get
        .objectFile[BiMap[String, Int]](s"/tmp/${id}/userIdToIxMap").first,
      productIdToIxMap = sc.get
        .objectFile[BiMap[String, Int]](s"/tmp/${id}/productIdToIxMap").first)
  }
}
