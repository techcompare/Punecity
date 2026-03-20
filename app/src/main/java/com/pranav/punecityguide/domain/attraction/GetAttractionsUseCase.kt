package com.pranav.punecityguide.domain.attraction

import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.repository.AttractionRepository
import kotlinx.coroutines.flow.Flow

class GetAttractionsUseCase(private val repository: AttractionRepository) {
    operator fun invoke(limit: Int = 50): Flow<List<Attraction>> {
        return repository.getTopAttractions(limit)
    }
}
