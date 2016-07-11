package apps.games.serious.preference.GUI

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Array
import sun.misc.ExtensionDependency

/**
 * Created by user on 7/1/16.
 */

/**
 * Interface for representation of all actions happening on the bord
 */
abstract class Action(var delay: Float){
    private val completeDependencies = mutableListOf<Action>()
    private val readyDependencies = mutableListOf<Action>()

    fun update(delta: Float){
        if(delay > 0){
            delay -= delta
        }
        if(delay <= 0){
            delay = 0f
            execute(delta)
        }
    }

    abstract fun execute(delta: Float)

    abstract fun isComplete(): Boolean

    fun executeWhenComplete(action: Action?){
        if (action != null) {
            completeDependencies.add(action)
        }
    }

    fun executeWhenReady(action: Action?){
        if (action != null) {
            readyDependencies.add(action)
        }
    }

    fun readyToExcute(): Boolean{
        resolveDependencies()
        return completeDependencies.isEmpty() && readyDependencies.isEmpty()
    }

    private fun resolveDependencies(){
        val completeIterator = completeDependencies.iterator()
        while(completeIterator.hasNext()){
            val dependency = completeIterator.next()
            if(dependency.isComplete()){
                completeIterator.remove()
            }
        }

        val readyIterator = readyDependencies.iterator()
        while(readyIterator.hasNext()){
            val dependency = readyIterator.next()
            if(dependency.readyToExcute() && dependency.delay <= 0){
                readyIterator.remove()
            }
        }
    }

}


/***
 * Card action manager. Keeps all
 * cards movements
 */
class ActionManager {
    internal val actions = mutableListOf<Action>()
    internal val pending = mutableListOf<Action>()

    fun update(delta: Float) {
        val pendingIterator = pending.iterator()
        while(pendingIterator.hasNext()){
            val action = pendingIterator.next()
            if(action.readyToExcute()){
                pendingIterator.remove()
                actions.add(action)
            }
        }

        val actionsIterator = actions.iterator()
        while(actionsIterator.hasNext()){
            val action = actionsIterator.next()
            action.update(delta)
            if(action.isComplete()){
                actionsIterator.remove()
            }
        }
    }

    fun add(action: Action){
        pending.add(action)
    }

    fun addAfterLastReady(action: Action){
        action.executeWhenReady(getLastAction())
        pending.add(action)
    }

    fun addAfterLastComplete(action: Action){
        action.executeWhenComplete(getLastAction())
        pending.add(action)
    }

    fun getLastAction(): Action?{
        if(pending.isNotEmpty()){
            return pending.last()
        }
        if(actions.isNotEmpty()){
            return actions.last()
        }
        return null
    }
}