package com.example.finalpaging

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.ProgressBar


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import retrofit2.http.GET
import retrofit2.http.Query

data class QuotesData(
    val count: Int,
    val lastItemIndex: Int,
    val page: Int,
    val results: List<Result>,
    val totalCount: Int,
    val totalPages: Int
)

data class Result(
    val _id: String,
    val author: String,
    val authorSlug: String,
    val content: String,
    val dateAdded: String,
    val dateModified: String,
    val length: Int,
    val tags: List<String>
)
interface QuotableApi {
    @GET("quotes")
    fun getQuotes(@Query("page") page: Int): Call<QuotesData>
}


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var progressBar: ProgressBar

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    private val BASE_URL = "https://api.quotable.io/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val quotableApi = retrofit.create(QuotableApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize RecyclerView and adapter
        recyclerView = findViewById(R.id.recyclerView)
        quotesAdapter = QuotesAdapter()

        // Set up LinearLayoutManager for RecyclerView
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Set up RecyclerView adapter
        recyclerView.adapter = quotesAdapter

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreItems()
                    }
                }
            }
        })

        // Load the initial page of quotes
        loadQuotes(currentPage)
    }

    private fun loadQuotes(page: Int) {
        // Show ProgressBar while loading
        progressBar.visibility = ProgressBar.VISIBLE

        // Make a network request to the Quotable API to get quotes for the specified page
        quotableApi.getQuotes(page).enqueue(object : Callback<QuotesData> {
            override fun onResponse(call: Call<QuotesData>, response: Response<QuotesData>) {
                // Hide ProgressBar after loading
                progressBar.visibility = ProgressBar.GONE

                if (response.isSuccessful) {
                    val quotesData = response.body()
                    quotesData?.let {
                        quotesAdapter.addAllQuotes(it.results)

                        // Check if it's the last page
                        isLastPage = it.page == it.totalPages

                        // Increment the page number
                        currentPage++
                    }
                }
                isLoading = false
            }

            override fun onFailure(call: Call<QuotesData>, t: Throwable) {
                // Hide ProgressBar on failure
                progressBar.visibility = ProgressBar.GONE

                // Handle failure
                isLoading = false
            }
        })
    }

    private fun loadMoreItems() {
        isLoading = true
        loadQuotes(currentPage)
    }
}

class QuotesAdapter : RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    private val quotesList: MutableList<Result> = mutableListOf()

    fun addAllQuotes(quotes: List<Result>) {
        quotesList.addAll(quotes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotesList[position]
        holder.bind(quote)
    }

    override fun getItemCount(): Int {
        return quotesList.size
    }

    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(quote: Result) {
            authorTextView.text = quote.author
            contentTextView.text = quote.content
        }
    }
}

