import org.apache.spark.{SparkConf, SparkContext}

object DocSimilarity {

  def jaccard(a: Set[String], b: Set[String]): Double = {
    val commonWords = a.intersect(b).size
    val allWords = a.union(b).size

    if (allWords == 0) {
      0.0
    } else {
      commonWords.toDouble / allWords
    }
  }

  def main(args: Array[String]): Unit = {
    require(args.length == 3,
      "Usage: DocSimilarity <inputDir> <stopwordsFile> <outputDir>")

    val inputDir = args(0)
    val stopwordsPath = args(1)
    val outputDir = args(2)
    val conf = new SparkConf().setAppName("DocSimilarity")
    val sc = new SparkContext(conf)

    try {
      // Load the stop words and share them with the workers.
      val stopWords = sc.textFile(stopwordsPath)
        .map(word => word.trim.toLowerCase)
        .filter(word => word.nonEmpty)
        .collect()
        .toSet
      val stopWordsBc = sc.broadcast(stopWords)

      // Task 1: Read each file and keep its name.
      val docs = sc.wholeTextFiles(inputDir).map { case (path, text) =>
        val fileName = new java.io.File(path).getName
        (fileName, text)
      }

      // Task 2: Clean the tokens. Keep punctuation attached.
      val tokens = docs.mapValues { text =>
        text.split("\\s+")
          .filter(word => word.nonEmpty)
          .map(word => word.toLowerCase)
          .filter(word => !stopWordsBc.value.contains(word))
          .filter(word => word.length > 1)
          .toList
      }

     //print the tokens
      tokens.filter { case (name, _) =>
          name == "doc10.txt" || name == "doc22.txt"
        }
        .take(2)
        .foreach { case (name, words) =>
          println(name + ": " + words.mkString("[", ", ", "]"))
        }

      // Task 3: Compare each pair once, using sets of words.
      val tokenSets = tokens.mapValues(words => words.toSet).cache()
      val pairs = tokenSets.cartesian(tokenSets)
        .filter { case ((d1, _), (d2, _)) => d1 < d2 }

      val scores = pairs.map { case ((d1, words1), (d2, words2)) =>
        (d1, d2, jaccard(words1, words2))
      }

      // Task 4: Highest scores first, then filenames for ties.
      val order = Ordering.by[(String, String, Double), (Double, String, String)] {
        case (d1, d2, score) => (-score, d1, d2)
      }
      val top20 = scores.takeOrdered(20)(order)

      val lines = top20.map { case (d1, d2, score) =>
        f"$d1,$d2,$score%.4f"
      }
      lines.foreach(println)

      // One partition produces one result part file.
      sc.parallelize(lines, 1).saveAsTextFile(outputDir)


      sc.stop()
    }


  }
}
