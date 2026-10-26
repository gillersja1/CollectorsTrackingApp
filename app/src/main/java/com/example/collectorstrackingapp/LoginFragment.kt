package com.example.collectorstrackingapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentLoginBinding

/**
 * Fragment handling user authentication (Login / Register).
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

        // Check if already logged in
        if (sharedPrefs.getBoolean("is_logged_in", false)) {
            findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
        }

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPrefs.edit().putBoolean("is_logged_in", true).putString("current_user", username).apply()
            Toast.makeText(requireContext(), "Welcome back, $username!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password to register", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPrefs.edit().putBoolean("is_logged_in", true).putString("current_user", username).apply()
            Toast.makeText(requireContext(), "Account created successfully for $username!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
