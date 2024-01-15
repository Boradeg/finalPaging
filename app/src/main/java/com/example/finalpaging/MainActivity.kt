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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.paging.PagingSource
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingState
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
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
    suspend fun getQuotes(@Query("page") page: Int): QuotesData
}

class QuotesPagingSource(private val quotableApi: QuotableApi) : PagingSource<Int, Result>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Result> {
        try {
            val page = params.key ?: 1
            val response = quotableApi.getQuotes(page)
            val quotes = response.results

            // Return results with next key (page) for paging
            return LoadResult.Page(
                data = quotes,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (quotes.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            // Handle error
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Result>): Int? {
        // Not needed for this example
        return null
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var progressBar: ProgressBar

    private val BASE_URL = "https://api.quotable.io/"
    private val quotableApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(QuotableApi::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        quotesAdapter = QuotesAdapter()

        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        // Attach the load state adapter
        recyclerView.adapter = quotesAdapter.withLoadStateHeaderAndFooter(
            header = QuotesLoadStateAdapter { quotesAdapter.retry() },
            footer = QuotesLoadStateAdapter { quotesAdapter.retry() }
        )

        val pager = Pager(PagingConfig(pageSize = 1)) {
            QuotesPagingSource(quotableApi)
        }

        val pagingDataFlow = pager.flow

        lifecycleScope.launch {
            pagingDataFlow.collectLatest { pagingData ->
                quotesAdapter.submitData(pagingData)
            }
        }
    }
}
class QuotesLoadStateAdapter(private val retry: () -> Unit) : LoadStateAdapter<QuotesLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
        return LoadStateViewHolder(view, retry)
    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(itemView: View, retry: () -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.loadStateProgressBar)
        private val textView: TextView = itemView.findViewById(R.id.loadStateTextView)
        private val retryButton: Button = itemView.findViewById(R.id.retryButton)

        init {
            retryButton.setOnClickListener { retry.invoke() }
        }

        fun bind(loadState: LoadState) {
            when (loadState) {
                is LoadState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    textView.text = "Loading..."
                    retryButton.visibility = View.GONE
                }
                is LoadState.Error -> {
                    progressBar.visibility = View.GONE
                    textView.text = "Error: ${loadState.error.localizedMessage}"
                    retryButton.visibility = View.VISIBLE
                }
                is LoadState.NotLoading -> {
                    progressBar.visibility = View.GONE
                    textView.text = ""
                    retryButton.visibility = View.GONE
                }
            }
        }
    }
}

class QuotesAdapter :
    PagingDataAdapter<Result, RecyclerView.ViewHolder>(QUOTES_COMPARATOR) {

    private val VIEW_TYPE_QUOTE = 0
    private val VIEW_TYPE_PROGRESS = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_QUOTE -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
                QuoteViewHolder(view)
            }
            VIEW_TYPE_PROGRESS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                ProgressViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QuoteViewHolder -> getItem(position)?.let { holder.bind(it) }
            is ProgressViewHolder -> holder.bind()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < itemCount && getItem(position) != null) {
            VIEW_TYPE_QUOTE
        } else {
            VIEW_TYPE_PROGRESS
        }
    }

    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)

        fun bind(quote: Result) {
            authorTextView.text = quote.author
            contentTextView.text = quote.content
        }
    }

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

        fun bind() {
            // You can customize the progress bar as needed
            progressBar.visibility = View.VISIBLE
        }
    }

    companion object {
        private val QUOTES_COMPARATOR = object : DiffUtil.ItemCallback<Result>() {
            override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
                return oldItem._id == newItem._id
            }

            override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
                return oldItem == newItem
            }
        }
    }
}
