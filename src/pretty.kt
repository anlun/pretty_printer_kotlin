package pretty

import java.util.ArrayList
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import java.util.LinkedList

// TODO: разобраться, что за проблема с nest
fun main(args : Array<String>) {
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

    /*
    val tree = generateTree(8, 5, 5, 100)
    //val tree = generateTree(5, 5, 5, 100)
    println("Press enter...")
    readLine()

    //println(pretty(50, showTree(tree)))
    //be_nonRecursive(15, 0, showTree(tree))
    */
}

abstract class PrimeDoc()
class PrimeNil() : PrimeDoc()
class PrimeBeside(
        val leftDoc: PrimeDoc
        , val rightDoc: PrimeDoc
) : PrimeDoc()
class PrimeNest (
        val nestSize: Int
        , val doc : PrimeDoc
) : PrimeDoc()
class PrimeText(
        val text : String
) : PrimeDoc()
class PrimeLine() : PrimeDoc()
class PrimeChoose(
        val leftDoc: () -> PrimeDoc //TODO: проанализировать на сколько помогло
        , val rightDoc: PrimeDoc
) : PrimeDoc()

abstract class Doc(val fitSize : Int)
class Nil() : Doc(0)
class Text(
        val text : String
        , var doc : Doc
) : Doc(text.length + doc.fitSize)
class Line(
        val nestSize: Int
        , var doc : Doc
) : Doc(0)

// ----- INTERFACE START -----
fun nil() = PrimeNil()
fun beside(val leftDoc: PrimeDoc, val rightDoc: PrimeDoc) = PrimeBeside(leftDoc, rightDoc)
fun nest(val nestSize: Int, val doc : PrimeDoc) = PrimeNest(nestSize, doc)
fun text(val text : String) = PrimeText(text)
fun line() = PrimeLine()

fun PrimeDoc.plus(val doc : PrimeDoc) : PrimeDoc {
    return beside(this, doc)
}

// сделать ленивым тут!
// Добавлена ленивость
fun group(val doc : PrimeDoc) = PrimeChoose({ flatten(doc) } , doc)

// Еще в интерфейсе pretty
// ----- INTERFACE END -----

fun flatten(val doc : PrimeDoc) : PrimeDoc =
        when (doc) {
            is PrimeNil    -> doc //PrimeNil()
            is PrimeBeside -> PrimeBeside(flatten(doc.leftDoc), flatten(doc.rightDoc))
            is PrimeNest   -> PrimeNest(doc.nestSize, flatten(doc.doc))
            is PrimeText   -> doc //PrimeText(doc.text)
            is PrimeLine   -> PrimeText(" ")
            is PrimeChoose -> flatten(doc.leftDoc())

            else -> throw IllegalArgumentException("Unknown PrimeDoc.")
        }

fun spaces(count : Int) : String {
    var builder = StringBuilder()
    for (i in 1..count) {
        builder .append(" ")
    }
    return builder.toString()
}

fun layout_rec(val doc : Doc) : String {
    when (doc) {
        is Nil  -> return ""
        is Text -> return doc.text + layout(doc.doc)
        is Line -> {
            return "\n" + spaces(doc.nestSize) + layout(doc.doc)
        }

        else -> throw IllegalArgumentException("Unknown Doc.")
    }
}

fun layout(val doc : Doc) : String {
    var resultBuilder = StringBuilder()

    var curDoc : Doc = doc
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

fun best(val width : Int, val alreadyOccupied : Int, val doc : PrimeDoc) : Doc {
    return be_nonRecursive(width, alreadyOccupied, doc)
}

abstract class StackDoc()
class StackNil  : StackDoc()
class StackLine(val nestSize : Int) : StackDoc()
class StackText(val text  : String) : StackDoc()

class StackDocDoc(val doc : Doc) : StackDoc()

fun moveNil(var docToMoveNil : Doc, val docToNilPlace : Doc) : Doc {
    if (docToMoveNil is Nil) {
        return docToNilPlace
    }

    var curSubtreeDoc = docToMoveNil
    while (true) {
        val curSubtreeDoc_val = curSubtreeDoc
        when (curSubtreeDoc_val) {
            is Text -> {
                if (curSubtreeDoc_val.doc is Nil) {
                    var curSubtreeDoc_var = curSubtreeDoc_val
                    curSubtreeDoc_val.doc = docToNilPlace
                    return docToMoveNil
                }

                curSubtreeDoc = curSubtreeDoc_val.doc
            }

            is Line -> {
                if (curSubtreeDoc_val.doc is Nil) {
                    var curSubtreeDoc_var = curSubtreeDoc_val
                    curSubtreeDoc_val.doc = docToNilPlace
                    return docToMoveNil
                }

                curSubtreeDoc = curSubtreeDoc_val.doc
            }

            else -> throw IllegalArgumentException("Incorrect Doc tree.")
        }
    }
}

fun docFromStack(var stack : ArrayList<StackDoc>) : Doc {
    var curResult : Doc = Nil()
    for (i in stack.indices) {
        val curPos = stack.size - i - 1
        val curElem = stack[curPos]

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

fun be_nonRecursive(val width : Int, val startAlreadyOccupied : Int, val doc : PrimeDoc, val nestSize : Int = 0) : Doc {
    var list = ArrayList<Pair<Int, PrimeDoc>>()
    list.add(Pair(nestSize, doc))
    var resultStack = ArrayList<StackDoc>()
    var alreadyOccupied = startAlreadyOccupied

    while (true) {
        if (list.empty) {
            return docFromStack(resultStack)
        }

        val head = list.head
        list.remove(0) // list = list.tail

        val nestSize = head!!.first
        val doc      = head.second

        when (doc) {
            is PrimeNil    -> resultStack.add(0, StackNil())
            is PrimeBeside -> {
                list.add(0, Pair(nestSize, doc.leftDoc)) // TODO: надо в начало добавлять!!!
                list.add(0, Pair(nestSize, doc.rightDoc))
            }
            is PrimeNest   -> {
                list.add(0, Pair(doc.nestSize + nestSize, doc.doc))
            }
            is PrimeText   -> {
                resultStack.add(0, StackText(doc.text))
                alreadyOccupied += doc.text.length()
            }
            is PrimeLine   -> {
                resultStack.add(0, StackLine(nestSize))
                alreadyOccupied = nestSize
            }

            is PrimeChoose -> {
                val leftDoc = be_nonRecursive(width, alreadyOccupied, doc.leftDoc(), nestSize)
                if (fits(width - alreadyOccupied, { leftDoc }) != null) {
                    resultStack.add(0, StackDocDoc(leftDoc))
                } else {
                    val rightDoc = be_nonRecursive(width, alreadyOccupied, doc.rightDoc, nestSize)
                    resultStack.add(0, StackDocDoc(rightDoc))
                }
            }

            else -> throw IllegalArgumentException("Unknown PrimeDoc.")
        }

    }
}

fun fits(val placeSize: Int, val docFunc: () -> Doc) : Doc? {
    if (placeSize < 0) return null

    val doc = docFunc()

    return if (placeSize > doc.fitSize) {
        doc
    } else {
        null
    }
}

fun pretty(val width : Int, doc : PrimeDoc) : String = layout(best(width, 0, doc))

// Utility
fun bracket(val openBracket: String, val doc : PrimeDoc, val closeBracket: String) : PrimeDoc =
        group(text(openBracket) + nest(2, line() + doc) + line() + text(closeBracket))