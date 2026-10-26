package com.example.collectorstrackingapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.data.AppDatabase
import com.example.collectorstrackingapp.databinding.FragmentLoginBinding
import com.example.collectorstrackingapp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment handling user authentication (Login / Register) with Room Database.
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

        val userDao = AppDatabase.getDatabase(requireContext()).userDao()

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val existingUser = userDao.getUserByUsername(username)
                withContext(Dispatchers.Main) {
                    if (existingUser != null && existingUser.passwordHash == password) {
                        sharedPrefs.edit().putBoolean("is_logged_in", true).putString("current_user", username).apply()
                        Toast.makeText(requireContext(), "Welcome back, $username!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                    } else {
                        Toast.makeText(requireContext(), "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password to register", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val existingUser = userDao.getUserByUsername(username)
                if (existingUser != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Username already exists. Please login.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    userDao.insertUser(User(username = username, passwordHash = password))
                    withContext(Dispatchers.Main) {
                        sharedPrefs.edit().putBoolean("is_logged_in", true).putString("current_user", username).apply()
                        Toast.makeText(requireContext(), "Account created successfully for $username!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
