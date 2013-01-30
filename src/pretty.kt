package pretty

import java.util.ArrayList

fun main(args : Array<String>) {
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

    //val tree = generateTree(10, 5, 5, 50)
    val tree = generateTree(5, 5, 5, 50)
    println("Press enter...")
    readLine()

    println(pretty(15, showTree(tree)))
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
        , val doc : Doc
) : Doc(text.size + doc.fitSize)
class Line(
        val nestSize: Int
        , val doc : Doc
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

fun layout(val doc : Doc) : String {
    when (doc) {
        is Nil  -> return ""
        is Text -> return doc.text + layout(doc.doc)
        is Line -> {
            return "\n" + spaces(doc.nestSize) + layout(doc.doc)
        }

        else -> throw IllegalArgumentException("Unknown Doc.")
    }
}

fun best(val width : Int, val alreadyOccupied: Int, val doc : PrimeDoc) : Doc {
    var list = ArrayList<Pair<Int, PrimeDoc>>(1)
    list.add(Pair(0, doc))
    return be(width, alreadyOccupied, list)
}

var beCounter = 0
fun be(val width : Int, val alreadyOccupied: Int, val docNestList: List<Pair<Int, PrimeDoc>>) : Doc {
    if (docNestList.empty) {
        return Nil()
    }

    //println(beCounter++)

    val head      = docNestList.head
    val nestSize = head!!.first
    val doc       = head.second

    when (doc) {
        is PrimeNil    -> return be(width, alreadyOccupied, docNestList.tail)
        is PrimeBeside -> {
            var list = ArrayList<Pair<Int, PrimeDoc>>(docNestList.size + 1)
            list.add(Pair(nestSize, doc.leftDoc))
            list.add(Pair(nestSize, doc.rightDoc))
            list += docNestList.tail
            return be(width, alreadyOccupied, list)
        }

        is PrimeNest -> {
            var list  = ArrayList<Pair<Int, PrimeDoc>>(docNestList.size)
            list.add(0, Pair(doc.nestSize + nestSize, doc.doc))
            list += docNestList.tail
            return be(width, alreadyOccupied, list)
        }
        is PrimeText -> return Text(doc.text, be(width, alreadyOccupied + doc.text.length(), docNestList.tail))
        is PrimeLine -> return Line(nestSize, be(width, nestSize, docNestList.tail))

        is PrimeChoose -> {
            //и тут замутить ленивость
            //UPDATE: lambda появилась
            var leftList = ArrayList<Pair<Int, PrimeDoc>>(docNestList.size)
            leftList.add(0, Pair(nestSize, doc.leftDoc()))
            leftList += docNestList.tail

            var rightList = ArrayList<Pair<Int, PrimeDoc>>(docNestList.size)
            rightList.add(0, Pair(nestSize, doc.rightDoc))
            rightList += docNestList.tail

            return better(width, alreadyOccupied
                    , { be(width, alreadyOccupied, leftList)  }
                    , { be(width, alreadyOccupied, rightList) }
                    )
        }

        else -> throw IllegalArgumentException("Unknown PrimeDoc.")
    }
}

fun better(val width : Int, val alreadyOccupied: Int, val leftDoc: () -> Doc, rightDoc: () -> Doc) : Doc =
    if (fits(width - alreadyOccupied, leftDoc)) {
//    if (true) {
//        val left_1 = rightDoc()
        // TODO: убрать, что leftDoc считается 2 раза - здесь и в fits. Но на производительность влияет не очень сильно
        leftDoc()
    } else {
        rightDoc()
    }

fun fits(val placeSize: Int, val docFunc: () -> Doc) : Boolean {
    if (placeSize < 0) return false

    val doc = docFunc()

    return placeSize > doc.fitSize
    /*
    when (doc) {
        is Nil  -> return true
        is Text -> return fits(placeSize - doc.text.size, { doc.doc })
        is Line -> return true

        else -> throw IllegalArgumentException("Unknown Doc.")
    }
    */
}

fun pretty(val width : Int, doc : PrimeDoc) : String = layout(best(width, 0, doc))

// Utility
fun bracket(val openBracket: String, val doc : PrimeDoc, val closeBracket: String) : PrimeDoc =
        group(text(openBracket) + nest(2, line() + doc) + line() + text(closeBracket))