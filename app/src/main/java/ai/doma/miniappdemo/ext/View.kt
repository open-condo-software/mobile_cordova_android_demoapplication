package ai.doma.miniappdemo.ext

import android.content.Context
import android.view.View
import androidx.annotation.Px

public inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

fun View.show(){
    this.visibility = View.VISIBLE
}

fun View.gone(){
    this.visibility = View.GONE
}

fun Context.pixels(dp: Int) = (dp * this.resources.displayMetrics.density + 0.5f).toInt()
fun View.pixels(dp: Int) = (dp * this.resources.displayMetrics.density + 0.5f).toInt()
