package ai.doma.miniappdemo.ext

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

inline fun <reified T : Fragment> FragmentManager.showFragment(fragment: T, tag: String = T::class.java.name): Unit {
    beginTransaction()
        .add(fragment, tag)
        .commitAllowingStateLoss()
}