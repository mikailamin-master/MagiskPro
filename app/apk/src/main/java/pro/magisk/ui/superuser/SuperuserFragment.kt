package pro.magisk.ui.superuser

import android.os.Bundle
import android.view.View
import pro.magisk.R
import pro.magisk.arch.BaseFragment
import pro.magisk.arch.viewModel
import pro.magisk.databinding.FragmentSuperuserMd2Binding
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import pro.magisk.core.R as CoreR

class SuperuserFragment : BaseFragment<FragmentSuperuserMd2Binding>() {

    override val layoutRes = R.layout.fragment_superuser_md2
    override val viewModel by viewModel<SuperuserViewModel>()

    override fun onStart() {
        super.onStart()
        activity?.title = resources.getString(CoreR.string.superuser)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.superuserList.apply {
            addEdgeSpacing(top = R.dimen.l_50, bottom = R.dimen.l1)
            addItemSpacing(R.dimen.l1, R.dimen.l_50, R.dimen.l1)
            fixEdgeEffect()
        }
    }

    override fun onPreBind(binding: FragmentSuperuserMd2Binding) {}

}
