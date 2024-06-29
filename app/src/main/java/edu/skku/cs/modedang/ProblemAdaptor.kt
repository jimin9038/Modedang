package edu.skku.cs.modedang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ProblemAdapter(private var problems: List<Problem>, private val onItemClick: (Problem) -> Unit) : RecyclerView.Adapter<ProblemAdapter.ProblemViewHolder>() {

    inner class ProblemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.problemTitle)
        val difficulty: TextView = itemView.findViewById(R.id.problemDifficulty)
        val submissionCount: TextView = itemView.findViewById(R.id.problemSubmissionCount)
        val acceptedRate: TextView = itemView.findViewById(R.id.problemAcceptedRate)
        val tags: TextView = itemView.findViewById(R.id.problemTags)

        init {
            itemView.setOnClickListener {
                onItemClick(problems[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_problem, parent, false)
        return ProblemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        val problem = problems[position]
        holder.title.text = problem.title
        holder.difficulty.text = problem.difficulty
        holder.submissionCount.text = "${problem.submissionCount} Submissions"
        holder.tags.text = problem.tags.joinToString(", ") { it.name }

        // Set text color based on acceptance rate
        val context = holder.itemView.context
        when {
            problem.acceptedRate > 0.60 -> {
                holder.acceptedRate.setTextColor(ContextCompat.getColor(context, R.color.skyBlue))
            }
            problem.acceptedRate > 0.30 -> {
                holder.acceptedRate.setTextColor(ContextCompat.getColor(context, android.R.color.black))
            }
            else -> {
                holder.acceptedRate.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
        }
        holder.acceptedRate.text = "%.2f%%".format(problem.acceptedRate * 100)
    }

    override fun getItemCount(): Int {
        return problems.size
    }

    fun updateProblems(newProblems: List<Problem>) {
        problems = newProblems
        notifyDataSetChanged()
    }
}
