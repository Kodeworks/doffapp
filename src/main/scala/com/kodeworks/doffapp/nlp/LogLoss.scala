package com.kodeworks.doffapp.nlp

//ripped from spark
/**
  * Class for log loss calculation (for classification).
  * This uses twice the binomial negative log likelihood, called "deviance" in Friedman (1999).
  *
  * The log loss is defined as:
  * 2 log(1 + exp(-2 y F(x)))
  * where y is a label in {-1, 1} and F(x) is the model prediction for features x.
  */
object LogLoss {
  def log1pExp(x: Double): Double = {
    if (x > 0d) {
      x + math.log1p(math.exp(-x))
    } else {
      math.log1p(math.exp(x))
    }
  }

  def clip(p: Double, eps: Double) =
    math.max(eps, math.min(1 - eps, p))


  def computeError(prediction: Double, label: Double, eps: Double = 0.0000005): Double = {
    val clipLabel = clip(label, eps)
    val clipPrediction = clip(prediction, eps)
    val margin = 2.0 * clipLabel * clipPrediction
    // The following is equivalent to 2.0 * log(1 + exp(-margin)) but more numerically stable.
    2.0 * log1pExp(-margin)
  }
}