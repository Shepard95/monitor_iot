package com.example.monitoriot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.monitoriot.databinding.FragmentLowLevelBinding

class LowLevelFragment : Fragment() {

    private var _binding: FragmentLowLevelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLowLevelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartFill.setOnClickListener {
            binding.tvStatus.text = "Estado: Llenando"
        }

        binding.btnStopFill.setOnClickListener {
            binding.tvStatus.text = "Estado: Inactiva"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}