package com.pranav.punecityguide.domain.attraction

import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import kotlinx.coroutines.flow.Flow

class SearchAttractionsUseCase(private val repository: AttractionRepository) {
    operator fun invoke(query: String): Flow<List<Attraction>> {
        return repository.searchAttractions(query)
    }
}
