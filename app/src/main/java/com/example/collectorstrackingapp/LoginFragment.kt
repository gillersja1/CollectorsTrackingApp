package com.example.collectorstrackingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.collectorstrackingapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragment handling user authentication (Login / Register / Password Reset) with Firebase Auth.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var auth: FirebaseAuth? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Hide action bar/toolbar and FAB on login screen
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        (activity as? MainActivity)?.findViewById<View>(R.id.fab)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore action bar and FAB when leaving login screen
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        (activity as? MainActivity)?.findViewById<View>(R.id.fab)?.visibility = View.VISIBLE
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }

        binding.passwordEditText.setOnEditorActionListener { _, actionId, event ->
            val isActionValid = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                                actionId == android.view.inputmethod.EditorInfo.IME_NULL
            val isEnterKey = (event != null && 
                             event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && 
                             event.action == android.view.KeyEvent.ACTION_DOWN)

            if (isActionValid || isEnterKey) {
                binding.loginButton.performClick()
                true
            } else {
                false
            }
        }

        binding.loginButton.setOnClickListener {
            val email = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firebaseAuth = auth
            if (firebaseAuth == null) {
                Toast.makeText(requireContext(), "Firebase Auth not initialized", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.loginButton.isEnabled = true
                        binding.registerButton.isEnabled = true
                    }
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                        try {
                            findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Authentication failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.registerButton.setOnClickListener {
            val email = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email and password to register", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firebaseAuth = auth
            if (firebaseAuth == null) {
                Toast.makeText(requireContext(), "Firebase Auth not initialized", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                        binding.loginButton.isEnabled = true
                        binding.registerButton.isEnabled = true
                    }
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                        try {
                            findNavController().navigate(R.id.action_LoginFragment_to_FirstFragment)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Registration failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.forgotPasswordTextView.setOnClickListener {
            val email = binding.usernameEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email above to reset password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val firebaseAuth = auth
            if (firebaseAuth == null) {
                Toast.makeText(requireContext(), "Firebase Auth not initialized", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (_binding != null) {
                        binding.progressBar.visibility = View.GONE
                    }
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
