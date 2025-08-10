package com.cocibolka.elbanquito.ui.acercade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cocibolka.elbanquito.databinding.FragmentAcercaDeBinding

class AcercaDeFragment : Fragment() {

    private var _binding: FragmentAcercaDeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val acercaDeViewModel =
            ViewModelProvider(this).get(AcercaDeViewModel::class.java)

        _binding = FragmentAcercaDeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textAcercaDe
        acercaDeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}