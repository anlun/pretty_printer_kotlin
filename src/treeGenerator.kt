package pretty

import java.util.ArrayList
import java.util.Random

class Tree(val text : String, val children : ArrayList<Tree>)

fun showTree(tree : Tree) : PrimeDoc = group(text(tree.text) + nest(tree.text.length, showBracket(tree.children)))

fun showBracket(trees : List<Tree>) : PrimeDoc {
    if (trees.empty) {
        return nil()
    }

    return  text("[") + nest("[".length, showTrees(trees)) + text("]")
}

fun showTrees(trees : List<Tree>) : PrimeDoc {
    val head = trees.head

    if (head == null) {
        return nil()
    }

    if (trees.tail.empty) {
        return showTree(head)
    }

    return showTree(head) + text(",") + line() + showTrees(trees.tail)
}

fun showTree_1(tree : Tree) : PrimeDoc {
    return text(tree.text) + showBracket_1(tree.children)
}

fun showBracket_1(trees : List<Tree>) : PrimeDoc {
    if (trees.empty) {
        return nil()
    }

    return bracket("[", showTrees_1(trees), "]")
}

fun showTrees_1(trees : List<Tree>) : PrimeDoc {
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

val maxChildAppearProbability = 100
fun generateTree(
        treeDepth : Int
        , childMaxCount : Int
        , maxLabelSize : Int
        , childAppearProbability : Int // Between 0 and 100 inclusive
) : Tree {
    // TODO: generate label
    val label = "aaa"
    //val label = "aaa12312141515251235235235"

    if (treeDepth <= 1) {
        return Tree("aaa", ArrayList<Tree>())
    }

    var children = ArrayList<Tree>()
    val randomGenerator = Random()
    for (i in 1..childMaxCount) {
        val rand = randomGenerator.nextInt(maxChildAppearProbability + 1)
        if (true) {
        //if (rand < childAppearProbability) {
            children.add(generateTree(treeDepth - 1, childMaxCount, maxLabelSize, childAppearProbability))
        }
    }

    return Tree(label, children)
}