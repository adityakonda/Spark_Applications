package com.sparkapplications

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

/**
  * Created by aditya on 12/7/2017.
  */
object ProductRevenueDataFrames {

  def main(args: Array[String]): Unit = {

    /*    SETTING UP SPARK CONFIGURATION   */
    val conf = new SparkConf()
    conf.setAppName("Product Revenue DataFrames")
    conf.setMaster("yarn-client")

    val sc = new SparkContext(conf)

    val hiveContext = new HiveContext(sc)
    import hiveContext.implicits._

    /*    READING FROM HDFS   */
    val ordersRdd = sc.textFile("/user/cloudera/retail_db/orders/")

    /*    READING FROM LOCAL FILE SYSTEM   */
    val productsRaw = Source.fromFile("/home/cloudera/aditya/data/retail_db/products/part-00000").getLines.toList
    val productRDD = sc.parallelize(productsRaw)

    val ordersDF = ordersRdd.map( order => {
      (order.split(",")(0).toInt, order.split(",")(1), order.split(",")(2).toInt, order.split(",")(3))
    }).toDF("order_id","order_date","order_customer_id","order_status")

    val productDF = productRDD.map( product => (product.split(",")(0).toInt, product.split(",")(2))).toDF("product_id","product_name")

    ordersDF.registerTempTable("orders")
    productDF.registerTempTable("products")

    hiveContext.sql("use retail_db_orc")

    val daily_revenue_per_product = hiveContext
                    .sql("SELECT o.order_date, p.product_name, sum(oi.order_item_subtotal) daily_revenue_per_product" +
                    "FROM orders o JOIN order_items oi " +
                    "ON o.order_id = oi.order_item_order_id" +
                    "JOIN products p ON p.product_id = oi.order_item_product_id" +
                    "WHERE o.order_status IN ('COMPLETE','CLOSED')" +
                    "GROUP BY o.order_date, p.product_name" +
                    "ORDER BY o.order_date, daily_revenue_per_product;")

    daily_revenue_per_product.show(5)

  }
}