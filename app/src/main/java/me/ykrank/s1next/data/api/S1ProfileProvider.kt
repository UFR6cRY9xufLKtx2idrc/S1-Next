package me.ykrank.s1next.data.api

import androidx.collection.LruCache
import com.github.ykrank.androidtools.util.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.cache.exmodel.BaseCache
import java.util.concurrent.ConcurrentHashMap

class S1ProfileProvider(private val s1Service: S1Service) : ProfileProvider {

    private val profileCache = LruCache<String, BaseCache<Profile>>(500)
    private val activeRequests = ConcurrentHashMap<String, Deferred<Profile?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profileUpdateFlow = MutableSharedFlow<Pair<String, Profile>>()
    override val profileUpdateFlow = _profileUpdateFlow.asSharedFlow()

    override fun getProfileCaches(userId: String): Profile? {
        val cache = profileCache.get(userId)
        if (cache == null) return null
        if (System.currentTimeMillis() - cache.time < CACHE_PROFILE_MILLS) {
            return cache.data
        }
        return null
    }

    override fun getProfiles(
        authorIds: List<String>,
        onProfileUpdate: ((uid: String, profile: Profile) -> Unit)?
    ) {
        authorIds.distinct().forEach { authorId ->
            scope.launch {
                val cachedProfile = getProfileCaches(authorId)
                if (cachedProfile != null) {
                    launch(Dispatchers.Main) {
                        onProfileUpdate?.invoke(authorId, cachedProfile)
                    }
                    return@launch
                }
                val deferred = activeRequests.computeIfAbsent(authorId) { id ->
                    L.d("ProfileProvider: No active request for $id. Starting new network call.")
                    async {
                        try {
                            fetchProfileFromApi(id)
                        } catch (e: Exception) {
                            L.report(e)
                            null
                        } finally {
                            L.d("ProfileProvider: Request for $id finished. Removing from active requests.")
                            activeRequests.remove(id)
                        }
                    }
                }

                try {
                    deferred.await()?.let { profile ->
                        profileCache.put(authorId, BaseCache(System.currentTimeMillis(), profile))
                        launch(Dispatchers.Main) {
                            onProfileUpdate?.invoke(authorId, profile)
                        }
                    }
                } catch (e: Exception) {
                    L.e("ProfileProvider: Error awaiting profile for id:$authorId", e)
                }
            }

        }
    }


    private suspend fun fetchProfileFromApi(uid: String): Profile? {
        try {
            val htmlResponse = s1Service.getProfileWeb(
                "${Api.BASE_URL}space-uid-${uid}.html", uid
            )
            val profile = Profile.fromHtml(htmlResponse)
            return profile
        } catch (e: Exception) {
            L.report(e)
            return null
        }
    }

    companion object {
        const val TAG = "S1Profile"
        const val CACHE_PROFILE_MILLS = 5 * 60 * 1_000L
    }
}