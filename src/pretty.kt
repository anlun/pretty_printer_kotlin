package pretty

import java.util.ArrayList

fun main(args : Array<String>) {
    //val a = arrayListOf(1, 2, 3)
    val tree = Tree("aaa"
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

    println(pretty(15, showTree(tree)))
}

abstract class PrimeDoc()
class PrimeNil() : PrimeDoc()
class PrimeBeside(
        val left_doc : PrimeDoc
        , val right_doc : PrimeDoc
) : PrimeDoc()
class PrimeNest (
        val nest_size : Int
        , val doc : PrimeDoc
) : PrimeDoc()
class PrimeText(
        val text : String
) : PrimeDoc()
class PrimeLine() : PrimeDoc()
class PrimeChoose(
        val left_doc : () -> PrimeDoc //TODO: проанализировать на сколько помогло
        , val right_doc : PrimeDoc
) : PrimeDoc()

abstract class Doc()
class Nil() : Doc()
class Text(
        val text : String
        , val doc : Doc
) : Doc()
class Line(
        val nest_size : Int
        , val doc : Doc
) : Doc()

fun nil() = PrimeNil()
fun beside(val left_doc : PrimeDoc, val right_doc : PrimeDoc) = PrimeBeside(left_doc, right_doc)
fun nest(val nest_size : Int, val doc : PrimeDoc) = PrimeNest(nest_size, doc)
fun text(val text : String) = PrimeText(text)
fun line() = PrimeLine()

fun PrimeDoc.plus(val doc : PrimeDoc) : PrimeDoc {
    return beside(this, doc)
}

//TODO: сделать ленивым тут!
fun group(val doc : PrimeDoc) = PrimeChoose({ flatten(doc) } , doc)

fun flatten(val doc : PrimeDoc) : PrimeDoc =
        when (doc) {
            is PrimeNil    -> doc //PrimeNil()
            is PrimeBeside -> PrimeBeside(flatten(doc.left_doc), flatten(doc.right_doc))
            is PrimeNest   -> PrimeNest(doc.nest_size, flatten(doc.doc))
            is PrimeText   -> doc //PrimeText(doc.text)
            is PrimeLine   -> PrimeText(" ")
            is PrimeChoose -> flatten(doc.left_doc())

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
            return "\n" + spaces(doc.nest_size) + layout(doc.doc)
        }

        else -> throw IllegalArgumentException("Unknown Doc.")
    }
}

fun best(val width : Int, val already_occupied : Int, val doc : PrimeDoc) : Doc {
    var list = ArrayList<Pair<Int, PrimeDoc>>(1)
    list.add(Pair(0, doc) )
    return be(width, already_occupied, list)
}

fun be(val width : Int, val already_occupied : Int, val doc_nest_list : List<Pair<Int, PrimeDoc>>) : Doc {
    if (doc_nest_list.empty) {
        return Nil()
    }

    val head      = doc_nest_list.head
    val nest_size = head!!.first
    val doc       = head.second

    when (doc) {
        is PrimeNil    -> return be(width, already_occupied, doc_nest_list.tail)
        is PrimeBeside -> {
            var list = ArrayList<Pair<Int, PrimeDoc>>(doc_nest_list.size + 1)
            list.add(Pair(nest_size, doc.left_doc))
            list.add(Pair(nest_size, doc.right_doc))
            list += doc_nest_list.tail
            return be(width, already_occupied, list)
        }

        is PrimeNest -> {
            var list  = ArrayList<Pair<Int, PrimeDoc>>(doc_nest_list.size)
            list.add(0, Pair(doc.nest_size + nest_size, doc.doc))
            list += doc_nest_list.tail
            return be(width, already_occupied, list)
        }
        is PrimeText -> return Text(doc.text, be(width, already_occupied + doc.text.length(), doc_nest_list.tail))
        is PrimeLine -> return Line(nest_size, be(width, nest_size, doc_nest_list.tail))

        is PrimeChoose -> {
            //TODO: и тут замутить ленивость
            //UPDATE: lambda появилась
            var left_list = ArrayList<Pair<Int, PrimeDoc>>(doc_nest_list.size)
            left_list.add(0, Pair(nest_size, doc.left_doc()))
            left_list += doc_nest_list.tail

            var right_list = ArrayList<Pair<Int, PrimeDoc>>(doc_nest_list.size)
            right_list.add(0, Pair(nest_size, doc.right_doc))
            right_list += doc_nest_list.tail

            return better(width, already_occupied
                    , be(width, already_occupied, left_list)
                    , { be(width, already_occupied, right_list) }
                    )
        }

        else -> throw IllegalArgumentException("Unknown PrimeDoc.")
    }
}

fun better(val width : Int, val already_occupied : Int, val left_doc : Doc, right_doc : () -> Doc) : Doc =
    if (fits(width - already_occupied, left_doc)) {
        left_doc
    } else {
        right_doc()
    }

fun fits(val place_size : Int, val doc : Doc) : Boolean {
    if (place_size < 0) return false

    when (doc) {
        is Nil  -> return true
        is Text -> return fits(place_size - doc.text.size, doc.doc)
        is Line -> return true

        else -> throw IllegalArgumentException("Unknown Doc.")
    }
}

fun pretty(val width : Int, doc : PrimeDoc) : String = layout(best(width, 0, doc))

// Utility
fun bracket(val open_bracket : String, val doc : PrimeDoc, val close_bracket : String) : PrimeDoc =
        group(text(open_bracket) + nest(2, line() + doc) + line() + text(close_bracket))

// Tree example
class Tree(val text : String, val children : ArrayList<Tree>)

fun showTree(val tree : Tree) : PrimeDoc = group(text(tree.text) + nest(tree.text.size, showBracket(tree.children)))

fun showBracket(val trees : List<Tree>) : PrimeDoc {
    if (trees.empty) {
        return nil()
    }

    return  text("[") + nest(1, showTrees(trees)) + text("]")
}

fun showTrees(val trees : List<Tree>) : PrimeDoc {
    val head = trees.head

    if (head == null) {
        return nil()
    }

    if (trees.tail.empty) {
        return showTree(head)
    }

    return showTree(head) + text(",") + line() + showTrees(trees.tail)
}

fun showTree_1(val tree : Tree) : PrimeDoc {
    return text(tree.text) + showBracket_1(tree.children)
}

fun showBracket_1(val trees : List<Tree>) : PrimeDoc {
    if (trees.empty) {
        return nil()
    }

    return bracket("[", showTrees_1(trees), "]")
}

fun showTrees_1(val trees : List<Tree>) : PrimeDoc {
    val head = trees.head
    val tail = trees.tail

    if (head == null) {
        return nil()
    }

    if (tail.size == 0) {
        return showTree(head)
    }
    return showTree(head) + text(",") + line() + showTrees(tail)
}

