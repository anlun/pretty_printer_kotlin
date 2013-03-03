package pretty

import java.util.ArrayList
import java.util.LinkedList
import java.util.Stack

fun main(args: Array<String>) {
    val doc = text("aaa") + nest(2, line() + text("bb") + nest(1, line() + text("cc")))
    println(pretty(15, doc))

    //val a = arrayListOf(1, 2, 3)
    val treeOld = Tree("aaa"
            , arrayListOf(
                Tree("bbbbb"
                     , arrayListOf(
                         Tree("ccc", arrayListOf())
                         , Tree("dd", arrayListOf())
                        )
                    )
                , Tree("eee", arrayListOf())
                , Tree("ffff"
                    , arrayListOf(
                        Tree("gg", arrayListOf())
                        , Tree("hhh", arrayListOf())
                        , Tree("ii", arrayListOf())
                        )
                    )
            )
    )
    println(pretty(15, showTree(treeOld)))

    //val tree = generateTree(9, 5, 5, 100)
    val tree = generateTree(5, 5, 5, 100)
    println("Press enter...")
    readLine()

    val startTime = System.nanoTime()
    println(pretty(50, showTree(tree)))
    val endTime   = System.nanoTime()
    val duration = (endTime - startTime) / Math.pow(10.0, 9.0)

    println("Duration: " + duration)
}

// ----- INTERFACE START -----

fun nil() = PrimeNil()
fun beside(leftDoc: PrimeDoc, rightDoc: PrimeDoc) = PrimeBeside(leftDoc, rightDoc)
fun nest(nestSize: Int, doc: PrimeDoc) = PrimeNest(nestSize, doc)
fun text(text: String) = PrimeText(text)
fun line() = PrimeLine()

fun group(doc: PrimeDoc) = PrimeChoose({ flatten(doc) } , doc)

fun pretty(width: Int, doc: PrimeDoc): String = layout(best(width, 0, doc))

// Utility
fun PrimeDoc.plus(doc: PrimeDoc): PrimeDoc {
    return beside(this, doc)
}

fun PrimeDoc.div(doc: PrimeDoc): PrimeDoc {
    return this + line() + doc
}

fun PrimeDoc.times(doc: PrimeDoc): PrimeDoc {
    return this + text(" ") + doc
}

fun bracket(openBracket: String, doc: PrimeDoc, closeBracket: String, nestSize: Int = 2): PrimeDoc =
        group(text(openBracket) + nest(nestSize, line() + doc) + line() + text(closeBracket))

//fun spread(docList : List<PrimeDoc>) : PrimeDoc =

// ----- INTERFACE END -----

abstract class PrimeDoc()
class PrimeNil() : PrimeDoc()
class PrimeBeside(
          val leftDoc: PrimeDoc
        , val rightDoc: PrimeDoc
) : PrimeDoc()
class PrimeNest (
          val nestSize: Int
        , val doc: PrimeDoc
) : PrimeDoc()
class PrimeText(
        val text: String
) : PrimeDoc()
class PrimeLine() : PrimeDoc()
class PrimeChoose(
          val leftDoc:  () -> PrimeDoc //TODO: проанализировать на сколько помогло
        , val rightDoc: PrimeDoc
) : PrimeDoc()

abstract class Doc(val fitSize: Int)
class Nil() : Doc(0)
class Text(
          val text: String
        , var doc:  Doc
) : Doc(text.length + doc.fitSize)
class Line(
          val nestSize: Int
        , var doc:      Doc
) : Doc(0)

fun flatten(doc: PrimeDoc): PrimeDoc =
        when (doc) {
            is PrimeNil    -> doc //PrimeNil()
            is PrimeBeside -> PrimeBeside(flatten(doc.leftDoc), flatten(doc.rightDoc))
            is PrimeNest   -> PrimeNest(doc.nestSize, flatten(doc.doc))
            is PrimeText   -> doc //PrimeText(doc.text)
            is PrimeLine   -> PrimeText(" ")
            is PrimeChoose -> flatten(doc.leftDoc())

            else -> throw IllegalArgumentException("Unknown PrimeDoc.")
        }

fun spaces(count: Int): String {
    val builder = StringBuilder()
    for (i in 1..count) {
        builder.append(" ")
    }
    return builder.toString()
}

fun layout(doc: Doc): String {
    val resultBuilder = StringBuilder()
    var curDoc: Doc   = doc

    while (true) {
        val workingDoc = curDoc

        when (workingDoc) {
            is Nil  -> break
            is Text -> {
                resultBuilder.append(workingDoc.text)
                curDoc = workingDoc.doc
            }
            is Line -> {
                resultBuilder.append("\n" + spaces(workingDoc.nestSize))
                curDoc = workingDoc.doc
            }

            else -> throw IllegalArgumentException("Unknown Doc.")
        }
    }

    return resultBuilder.toString()
}

abstract class StackDoc()
class StackNil : StackDoc()
class StackLine(val nestSize: Int   ) : StackDoc()
class StackText(val text:     String) : StackDoc()

class StackDocDoc(val doc: Doc) : StackDoc()

fun moveNil(docToMoveNil: Doc, docForNilPlace: Doc): Doc {
    if (docToMoveNil is Nil) {
        return docForNilPlace
    }

    var curSubtreeDoc = docToMoveNil
    while (true) {
        val curSubtreeDoc_val = curSubtreeDoc
        when (curSubtreeDoc_val) {
            is Text -> {
                if (curSubtreeDoc_val.doc is Nil) {
                    curSubtreeDoc_val.doc = docForNilPlace
                    return docToMoveNil
                }

                curSubtreeDoc = curSubtreeDoc_val.doc
            }

            is Line -> {
                if (curSubtreeDoc_val.doc is Nil) {
                    curSubtreeDoc_val.doc = docForNilPlace
                    return docToMoveNil
                }

                curSubtreeDoc = curSubtreeDoc_val.doc
            }

            else -> throw IllegalArgumentException("Incorrect Doc tree.")
        }
    }
}

fun docFromStack(stack: ArrayList<StackDoc>): Doc {
    var curResult: Doc = Nil()

    for (curElem in stack) {
        when (curElem) {
            is StackNil  -> {}
            is StackText -> curResult = Text(curElem.text    , curResult)
            is StackLine -> curResult = Line(curElem.nestSize, curResult)

            is StackDocDoc -> curResult = moveNil(curElem.doc, curResult)

            else -> throw IllegalArgumentException("Unknown stack element")
        }
    }

    return curResult
}

fun best(width: Int, startAlreadyOccupied: Int, doc : PrimeDoc, nestSize : Int = 0): Doc {
    val workStack = Stack<Pair<Int, PrimeDoc>>()
    workStack.push(Pair(nestSize, doc))
    val resultStack = ArrayList<StackDoc>()
    var alreadyOccupied = startAlreadyOccupied

    while (true) {
        if (workStack.empty) {
            return docFromStack(resultStack)
        }

        val head     = workStack.pop()
        val nestSize = head!!.first
        val doc      = head.second

        when (doc) {
            is PrimeNil    -> resultStack.add(StackNil())
            is PrimeBeside -> {
                workStack.push(Pair(nestSize, doc.leftDoc))
                workStack.push(Pair(nestSize, doc.rightDoc))
            }
            is PrimeNest   -> {
                workStack.push(Pair(doc.nestSize + nestSize, doc.doc))
            }
            is PrimeText   -> {
                resultStack.add(StackText(doc.text))
                alreadyOccupied += doc.text.length()
            }
            is PrimeLine   -> {
                resultStack.add(StackLine(nestSize))
                alreadyOccupied = nestSize
            }

            is PrimeChoose -> {
                val leftDoc = best(width, alreadyOccupied, doc.leftDoc(), nestSize)
                if (fits(width - alreadyOccupied, { leftDoc }) != null) {
                    resultStack.add(StackDocDoc(leftDoc))
                } else {
                    val rightDoc = best(width, alreadyOccupied, doc.rightDoc, nestSize)
                    resultStack.add(StackDocDoc(rightDoc))
                }
            }

            else -> throw IllegalArgumentException("Unknown PrimeDoc.")
        }
    }
}

fun fits(placeSize: Int, docFunc: () -> Doc): Doc? {
    if (placeSize < 0) return null

    val doc = docFunc()

    return if (placeSize > doc.fitSize) {
        doc
    } else {
        null
    }
}
