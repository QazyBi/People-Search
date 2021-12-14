package service
import scala.language.postfixOps

object CosineSimilarity {
  // inspired by: https://gist.github.com/reuben-sutton/2932974
  def cosineSimilarity(emb1: List[Double], emb2: List[Double]): Double = {
    dotProduct(emb1, emb2) / (magnitude(emb1) * magnitude(emb2))
  }
  def magnitude(x: List[Double]): Double = {
    math.sqrt(x map (i => i * i) sum)
  }

  def dotProduct(x: List[Double], y: List[Double]): Double = {
    (for ((a, b) <- x zip y) yield a * b) sum
  }
}
