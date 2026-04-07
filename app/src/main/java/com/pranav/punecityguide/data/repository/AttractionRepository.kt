package com.pranav.punecityguide.data.repository

import android.content.Context
import com.pranav.punecityguide.AppConfig
import com.pranav.punecityguide.data.database.PuneCityDatabase
import com.pranav.punecityguide.data.model.Attraction
import com.pranav.punecityguide.data.service.RemoteAttractionsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AttractionRepository(context: Context) {
    private val dao = PuneCityDatabase.getInstance(context).attractionDao()
    private val remoteService = RemoteAttractionsService()

    suspend fun getAllAttractions(): List<Attraction> = withContext(Dispatchers.IO) {
        try {
            // 1. Try to fetch from remote GitHub JSON
            val remoteAttractions = remoteService.fetchAttractions(AppConfig.Features.GITHUB_DATA_URL)
            
            if (remoteAttractions.isNotEmpty()) {
                // Sync remote with DB (overwrite or add)
                dao.insertAll(remoteAttractions)
                return@withContext dao.getAllAttractions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback to local DB and defaults
        val dbList = dao.getAllAttractions()
        val defaults = getDefaultAttractions()
        if (dbList.size < defaults.size) {
            // New defaults added, sync them
            dao.insertAll(defaults)
            return@withContext dao.getAllAttractions()
        }
        dbList
    }

    fun getCategories(): List<String> = listOf("Heritage", "Nature", "Entertainment", "Spiritual", "Education", "Food", "Katta Culture", "Hidden Gems")

    fun getTopAttractions(limit: Int): List<Attraction> {
        return getDefaultAttractions().sortedByDescending { it.rating }.take(limit)
    }
    
    fun getAttractionsByTag(tag: String): List<Attraction> {
        return getDefaultAttractions().filter { it.tags.contains(tag) }
    }
    
    suspend fun getAttractionById(id: Int): Attraction? {
        return dao.getAllAttractions().find { it.id == id } ?: getDefaultAttractions().find { it.id == id }
    }

    private fun getDefaultAttractions(): List<Attraction> = listOf(
        Attraction(
            id = 1,
            name = "Shaniwar Wada",
            category = "Heritage",
            description = "Shaniwar Wada was the seat of the Peshwas of the Maratha Empire until 1818. This peak of Maratha culture in Pune features massive stone walls and a scenic garden. It's famous for its light and sound show.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Shaniwar_Wada_Pune.jpg/800px-Shaniwar_Wada_Pune.jpg",
            rating = 4.6,
            latitude = 18.5195,
            longitude = 73.8553,
            address = "Shaniwar Peth, Pune",
            tags = listOf("History", "Architecture", "Must Visit"),
            reviews = 15200,
            visitDuration = "1-2 hours",
            location = "Central Pune",
            timings = "8:00 AM - 6:30 PM",
            entryFee = "₹25",
            localName = "शनिवार वाडा",
            bestTime = "Evening (during Sound & Light show)"
        ),
        Attraction(
            id = 2,
            name = "Aga Khan Palace",
            category = "Heritage",
            description = "Built by Sultan Muhammed Shah Aga Khan III in 1892, this palace is a memorial to Mahatma Gandhi who was imprisoned here. It's a majestic building with Italian arches and spacious lawns.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Aga_Khan_Palace_Pune_01.jpg/800px-Aga_Khan_Palace_Pune_01.jpg",
            rating = 4.7,
            latitude = 18.5524,
            longitude = 73.9015,
            address = "Nagar Road, Kalyani Nagar, Pune",
            tags = listOf("History", "Peaceful", "Museum"),
            reviews = 12400,
            visitDuration = "2-3 hours",
            location = "Kalyani Nagar",
            timings = "9:00 AM - 5:30 PM",
            entryFee = "₹25",
            localName = " आगा खान पॅलेस",
            bestTime = "Morning (10:00 AM) or late afternoon"
        ),
        Attraction(
            id = 3,
            name = "Sinhagad Fort",
            category = "Nature",
            description = "Sinhagad is a hill fortress with a history of many battles. The trek to the top is rewarded with breathtaking views and traditional local food like Pithla Bhakri. A favorite weekend getaway.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Sinhagad_Fort_Pune.jpg/800px-Sinhagad_Fort_Pune.jpg",
            rating = 4.8,
            latitude = 18.3663,
            longitude = 73.7554,
            address = "Sinhagad Ghat Road, Thoptewadi",
            tags = listOf("Trekking", "Views", "History"),
            reviews = 45000,
            visitDuration = "3-4 hours",
            location = "Outskirts",
            timings = "5:00 AM - 6:00 PM",
            entryFee = "₹50 (Vehicle)"
        ),
        Attraction(
            id = 4,
            name = "Dagdusheth Halwai Ganpati",
            category = "Spiritual",
            description = "The most popular temple in Pune dedicated to Lord Ganesha. The 10-day Ganeshotsav festival here is famous across India. The idol is adorned with gold and beautiful decorations year-round.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0e/Dagadusheth_Halwai_Ganapati_Temple%2C_Pune.jpg/800px-Dagadusheth_Halwai_Ganapati_Temple%2C_Pune.jpg",
            rating = 4.9,
            latitude = 18.5164,
            longitude = 73.8559,
            address = "Budhwar Peth, Pune",
            tags = listOf("Temple", "Culture", "Crowded"),
            reviews = 51200,
            visitDuration = "1 hour",
            location = "Budhwar Peth",
            timings = "6:00 AM - 11:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 5,
            name = "Pune Okayama Friendship Garden",
            category = "Nature",
            description = "Inspired by the 300-year-old Korakuen garden in Japan, it’s one of the largest gardens in Pune. A perfect spot for a tranquil walk with streams, koi ponds, and Zen aesthetics.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/15/Friendship_garden%2Cpune.jpg/800px-Friendship_garden%2Cpune.jpg",
            rating = 4.6,
            latitude = 18.4916,
            longitude = 73.8329,
            address = "Sinhagad Road, Dattawadi, Pune",
            tags = listOf("Garden", "Relaxation", "Photography"),
            reviews = 18300,
            visitDuration = "1.5 hours",
            location = "Dattawadi",
            timings = "6:00 AM - 10:30 AM, 4:00 PM - 8:00 PM",
            entryFee = "₹10"
        ),
        Attraction(
            id = 6,
            name = "Osho Ashram",
            category = "Spiritual",
            description = "An international meditation resort in Koregaon Park. Attracts visitors from all over the world. Beautiful gardens and a tranquil atmosphere for soul searching.",
            image = "https://images.unsplash.com/photo-1544256718-3baf237f39d0?q=80&w=600&auto=format&fit=crop",
            rating = 4.4,
            latitude = 18.5372,
            longitude = 73.8938,
            address = "Koregaon Park, Pune",
            tags = listOf("Meditation", "International", "Wellness"),
            reviews = 9800,
            visitDuration = "Half day",
            location = "Koregaon Park",
            timings = "Varies by program",
            entryFee = "Premium"
        ),
        Attraction(
            id = 7,
            name = "Parvati Hill",
            category = "Heritage",
            description = "A hillock at 2,100 feet above sea level with several temples including the Peshwa museum. It offers the best panoramic view of Pune city. A popular morning fitness spot.",
            image = "https://images.unsplash.com/photo-1589182397057-0131109968a3?q=80&w=600&auto=format&fit=crop",
            rating = 4.7,
            latitude = 18.4965,
            longitude = 73.8447,
            address = "Parvati Hill, Pune",
            tags = listOf("Views", "Temple", "History"),
            reviews = 32000,
            visitDuration = "1 hour",
            location = "Parvati",
            timings = "5:00 AM - 8:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 8,
            name = "Raja Dinkar Kelkar Museum",
            category = "Heritage",
            description = "Contains the extensive collection of Dr. Dinkar G. Kelkar, dedicated to his son. Features thousands of items including instruments, textiles, and artifacts from the 14th century.",
            image = "https://images.unsplash.com/photo-1610448106148-3c4805c93c44?q=80&w=600&auto=format&fit=crop",
            rating = 4.6,
            latitude = 18.5113,
            longitude = 73.8540,
            address = "Shukrawar Peth, Pune",
            tags = listOf("Museum", "Art", "Culture"),
            reviews = 8500,
            visitDuration = "2-3 hours",
            location = "Central Pune",
            timings = "10:00 AM - 5:30 PM",
            entryFee = "₹50"
        ),
        Attraction(
            id = 9,
            name = "Savitribai Phule Pune University",
            category = "Education",
            description = "Often called the 'Oxford of the East', the university campus is spread over 411 acres. The colonial-era architecture and lush greenery make for a scenic walk.",
            image = "https://images.unsplash.com/photo-1541339907198-e08756eaa539?q=80&w=600&auto=format&fit=crop",
            rating = 4.8,
            latitude = 18.5519,
            longitude = 73.8181,
            address = "Ganeshkhind Road, Pune",
            tags = listOf("University", "Campus", "Scenic"),
            reviews = 25000,
            visitDuration = "2 hours",
            location = "Ganeshkhind",
            timings = "Open all days",
            entryFee = "Free"
        ),
        Attraction(
            id = 10,
            name = "Vetal Tekdi",
            category = "Nature",
            description = "The highest point within Pune city limits. It's a popular trekking and jogging spot known for its quarry and the Vetal temple at the top. Great for sunsets.",
            image = "https://images.unsplash.com/photo-1544256718-3baf237f39d0?q=80&w=600&auto=format&fit=crop",
            rating = 4.7,
            latitude = 18.5284,
            longitude = 73.8184,
            address = "Kothrud, Pune",
            tags = listOf("Trekking", "Nature", "Sunset"),
            reviews = 15000,
            visitDuration = "1.5 hours",
            location = "Kothrud",
            timings = "24 hours",
            entryFee = "Free"
        ),
        Attraction(
            id = 11,
            name = "Pashan Lake",
            category = "Nature",
            description = "A peaceful man-made lake from the British era. Today, it serves as a critical bird-watching hub and a favorite morning walk spot for Kothrud and Pashan locals.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Pashan_Lake.jpg/800px-Pashan_Lake.jpg",
            rating = 4.2,
            latitude = 18.5367,
            longitude = 73.7915,
            address = "Pashan, Pune",
            tags = listOf("Birds", "Peaceful", "Nature"),
            reviews = 7200,
            visitDuration = "1 hour",
            location = "Pashan",
            timings = "6:00 AM - 11:00 AM, 4:00 PM - 7:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 12,
            name = "Mulshi Dam",
            category = "Nature",
            description = "A massive irrigation dam on the Mula river. The surrounding lush green hills and misty atmosphere make it Pune's favorite monsoon getaway.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Mulshi_Dam_Pune.jpg/800px-Mulshi_Dam_Pune.jpg",
            rating = 4.8,
            latitude = 18.5244,
            longitude = 73.4116,
            address = "Mulshi Taluka, Pune",
            tags = listOf("Dam", "Monsoon", "Drive"),
            reviews = 28000,
            visitDuration = "Full day",
            location = "Mulshi",
            timings = "Open all days",
            entryFee = "Free"
        ),
        Attraction(
            id = 13,
            name = "Koregaon Park",
            category = "Food",
            description = "The cosmopolitan heart of Pune. Known for its banyan-shaded lanes, 'Osho' influence, and iconic eateries like German Bakery and Chulham.",
            image = "https://images.pexels.com/photos/1547636/pexels-photo-1547636.jpeg?auto=compress&cs=tinysrgb&w=600", // High-end vibe
            rating = 4.6,
            latitude = 18.5362,
            longitude = 73.8940,
            address = "Koregaon Park, Pune",
            tags = listOf("Nightlife", "Dining", "Trendy"),
            reviews = 15000,
            visitDuration = "3 hours",
            location = "Koregaon Park",
            timings = "10:00 AM - 1:00 AM",
            entryFee = "Varies"
        ),
        Attraction(
            id = 14,
            name = "FC Road Market",
            category = "Entertainment",
            description = "Fergusson College Road is the hub of student activity. High-street shopping, local snacks (vada pav, bhel), and a vibrant young energy define this street.",
            image = "https://images.unsplash.com/photo-1555529733-0e67056058e1?q=80&w=600&auto=format&fit=crop",
            rating = 4.5,
            latitude = 18.5245,
            longitude = 73.8407,
            address = "Shivajinagar, Pune",
            tags = listOf("Shopping", "Street Food", "Vibe"),
            reviews = 42000,
            visitDuration = "2 hours",
            location = "Shivajinagar",
            timings = "11:00 AM - 10:30 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 15,
            name = "Camp / MG Road",
            category = "Food",
            description = "The Pune Cantonment area with colonial flavor. MG Road is famous for its old-school bakeries, iconic restaurants like Marz-o-Rin, and weekend shopping.",
            image = "https://images.unsplash.com/photo-1582719508461-905c673771fd?q=80&w=600&auto=format&fit=crop",
            rating = 4.6,
            latitude = 18.5147,
            longitude = 73.8789,
            address = "MG Road, Camp, Pune",
            tags = listOf("Shopping", "Bakery", "Culture"),
            reviews = 35000,
            visitDuration = "3 hours",
            location = "Pune Camp",
            timings = "10:00 AM - 10:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 16,
            name = "Pune Tribal Museum",
            category = "Heritage",
            description = "A unique museum that showcases the life, culture, and arts of Maharashtra's tribal communities. Features weapons, ornaments, and musical instruments.",
            image = "https://images.unsplash.com/photo-1574007557239-afe4470aa40b?q=80&w=600&auto=format&fit=crop",
            rating = 4.3,
            latitude = 18.5284,
            longitude = 73.8739,
            address = "Bund Garden Road, Pune",
            tags = listOf("Tribal", "Education", "Art"),
            reviews = 3400,
            visitDuration = "1 hour",
            location = "Bund Garden",
            timings = "10:00 AM - 5:00 PM",
            entryFee = "₹20"
        ),
        Attraction(
            id = 17,
            name = "Rajiv Gandhi Zoological Park",
            category = "Entertainment",
            description = "Located at Katraj, it's a huge 130-acre zoo. Features a popular snake park and is home to several rescued animals including white tigers and leopards.",
            image = "https://images.unsplash.com/photo-1534567153574-2b12153a87f0?q=80&w=600&auto=format&fit=crop",
            rating = 4.5,
            latitude = 18.4526,
            longitude = 73.8596,
            address = "Katraj, Pune",
            tags = listOf("Zoo", "Family", "Animals"),
            reviews = 68000,
            visitDuration = "3-4 hours",
            location = "Katraj",
            timings = "9:30 AM - 6:00 PM",
            entryFee = "₹40"
        ),
        Attraction(
            id = 18,
            name = "Empress Botanical Garden",
            category = "Nature",
            description = "A historical garden spread over 39 acres. Hosts annual flower shows and is a favorite for environmental lovers and morning walkers. Very peaceful.",
            image = "https://images.unsplash.com/photo-1541339907198-e08756eaa539?q=80&w=600&auto=format&fit=crop",
            rating = 4.4,
            latitude = 18.5134,
            longitude = 73.8927,
            address = "Kavade Mala, Ghorpadi",
            tags = listOf("Garden", "Historical", "Flowers"),
            reviews = 12500,
            visitDuration = "1.5 hours",
            location = "Kavade Mala",
            timings = "9:30 AM - 6:30 PM",
            entryFee = "₹20"
        ),
        Attraction(
            id = 19,
            name = "National Defence Academy (NDA)",
            category = "Education",
            description = "The premier joint training institution and the cradle of military leadership. The Sudan Block architecture is breathtaking. Note: Permission required for visit.",
            image = "https://images.unsplash.com/photo-1544073359-54313f890f84?q=80&w=600&auto=format&fit=crop",
            rating = 4.9,
            latitude = 18.4795,
            longitude = 73.7486,
            address = "Khadakwasla, Pune",
            tags = listOf("Military", "Pride", "Majestic"),
            reviews = 11000,
            visitDuration = "Limited Access",
            location = "Khadakwasla",
            timings = "Restricted Access",
            entryFee = "N/A"
        ),
        Attraction(
            id = 20,
            name = "Khadakwasla Dam",
            category = "Nature",
            description = "One of the main sources of water for Pune. The shores are a popular hangout for locals to enjoy corn-on-the-cob and street food while watching the sunset.",
            image = "https://images.unsplash.com/photo-1510414842594-a61c69b5ae57?q=80&w=600&auto=format&fit=crop",
            rating = 4.4,
            latitude = 18.4414,
            longitude = 73.7656,
            address = "Khadakwasla, Pune",
            tags = listOf("Sunset", "Water", "Snacks"),
            reviews = 55000,
            visitDuration = "1 hour",
            location = "Khadakwasla",
            timings = "5:00 AM - 8:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 21,
            name = "Dehu Gatha Temple",
            category = "Spiritual",
            description = "The birthplace of Saint Tukaram, a famous 17th-century poet-saint. The temple complex by the river Indrayani is beautiful, specially the Gatha carvings.",
            image = "https://images.unsplash.com/photo-1590059530279-b79e2c4314db?q=80&w=600&auto=format&fit=crop",
            rating = 4.8,
            latitude = 18.7183,
            longitude = 73.7690,
            address = "Dehu, Pimpri-Chinchwad",
            tags = listOf("Saint", "Spiritual", "River"),
            reviews = 42000,
            visitDuration = "2 hours",
            location = "Dehu",
            timings = "6:30 AM - 8:30 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 22,
            name = "Appu Ghar",
            category = "Entertainment",
            description = "The first amusement park in Pune/PCMC. Known as the 'Mini Disneyland' of Pune, it offers a variety of rides and joy for kids and families.",
            image = "https://images.unsplash.com/photo-1513553404607-988bf2703777?q=80&w=600&auto=format&fit=crop",
            rating = 4.2,
            latitude = 18.6606,
            longitude = 73.7674,
            address = "Nigdi, Pimpri-Chinchwad",
            tags = listOf("Kids", "Family", "Amusement"),
            reviews = 18500,
            visitDuration = "3 hours",
            location = "Nigdi",
            timings = "12:00 PM - 8:30 PM",
            entryFee = "₹350"
        ),
        Attraction(
            id = 23,
            name = "Pune National War Memorial",
            category = "Heritage",
            description = "Built by the citizens of Pune to honor military martyrs from post-independent India. Features a MiG-23 aircraft and a scale model of INS Vikrant.",
            image = "https://images.unsplash.com/photo-1544073359-54313f890f84?q=80&w=600&auto=format&fit=crop",
            rating = 4.8,
            latitude = 18.5255,
            longitude = 73.8821,
            address = "Pune Cantonment",
            tags = listOf("Patriotic", "Military", "Respect"),
            reviews = 9200,
            visitDuration = "1 hour",
            location = "Pune Camp",
            timings = "9:00 AM - 5:30 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 24,
            name = "Katraj Jain Temple",
            category = "Spiritual",
            description = "A magnificent Jain temple built of Rajasthani marble. Located on a hill, it offers a peaceful environment and great city views.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cf/Katraj_Jain_Temple_Pune.jpg/800px-Katraj_Jain_Temple_Pune.jpg",
            rating = 4.7,
            latitude = 18.4418,
            longitude = 73.8647,
            address = "Katraj Kondhwa Road",
            tags = listOf("Temple", "Marble", "Views"),
            reviews = 16000,
            visitDuration = "1 hour",
            location = "Katraj",
            timings = "6:00 AM - 9:00 PM",
            entryFee = "Free"
        ),
        Attraction(
            id = 25,
            name = "Bedse Caves",
            category = "Heritage",
            description = "Ancient Buddhist rock-cut caves dating back to the 1st century BC. Less crowded than Karla/Bhaja, they offer amazing architecture and stupas.",
            image = "https://images.unsplash.com/photo-1610443906322-cff3be3927cd?q=80&w=600&auto=format&fit=crop",
            rating = 4.6,
            latitude = 18.7235,
            longitude = 73.5350,
            address = "Maval, near Kamshet",
            tags = listOf("Caves", "Buddhist", "Offbeat"),
            reviews = 5200,
            visitDuration = "2 hours",
            location = "Maval",
            timings = "8:00 AM - 5:00 PM",
            entryFee = "₹20",
            localName = "बेडसे लेणी",
            bestTime = "Early morning during monsoons"
        ),
        Attraction(
            id = 26,
            name = "Goodluck Cafe",
            category = "Katta Culture",
            description = "Pune's most legendary Irani cafe. Established in 1935, it's the cultural epicenter of FC Road, famous for Bun Maska, Omelette, and endless debates over tea.",
            image = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Cafe_Good_Luck_Pune.jpg/800px-Cafe_Good_Luck_Pune.jpg",
            rating = 4.7,
            latitude = 18.5173,
            longitude = 73.8415,
            address = "Fergusson College Road, Pune",
            tags = listOf("Irani Cafe", "Iconic", "Budget"),
            reviews = 85000,
            visitDuration = "1 hour",
            location = "Deccan",
            timings = "7:30 AM - 11:30 PM",
            entryFee = "Varies",
            localName = "कॅफे गुडलक",
            bestTime = "Breakfast (8:00 AM)"
        ),
        Attraction(
            id = 27,
            name = "Vetal Tekdi (Sunrise)",
            category = "Hidden Gems",
            description = "The ultimate 'Secret Spot' for Punekars. Hike up before dawn to see the city wake up. The panoramic view of Pune at sunrise is mystical, often with fog rolling over the quarry.",
            image = "https://images.unsplash.com/photo-1544256718-3baf237f39d0?q=80&w=600&auto=format&fit=crop",
            rating = 4.9,
            latitude = 18.5284,
            longitude = 73.8184,
            address = "Kothrud/Shivajinagar boundary",
            tags = listOf("Sunrise", "Secret", "Photography"),
            reviews = 5000,
            visitDuration = "2 hours",
            location = "Kothrud",
            timings = "24 Hours (Sunrise recommended)",
            entryFee = "Free",
            localName = "वेताळ टेकडी (सूर्योदय)",
            bestTime = "5:30 AM - 6:30 AM"
        ),
        Attraction(
            id = 28,
            name = "Vaishali",
            category = "Katta Culture",
            description = "The heartbeat of FC Road. Every Pune student has a memory of the filter coffee and SPDP here. It's not just a restaurant; it's a Pune institution for the youth.",
            image = "https://images.pexels.com/photos/2474661/pexels-photo-2474661.jpeg?auto=compress&cs=tinysrgb&w=600", // Representative Indian Cafe
            rating = 4.8,
            latitude = 18.5204,
            longitude = 73.8415,
            address = "FC Road, Pune",
            tags = listOf("Breakfast", "Historic", "Student Life"),
            reviews = 95000,
            visitDuration = "1 hour",
            location = "Deccan",
            timings = "7:00 AM - 11:00 PM",
            entryFee = "Varies",
            localName = "वैशाली",
            bestTime = "Breakfast or evening snacks"
        ),
        Attraction(
            id = 29,
            name = "Arai Hill",
            category = "Hidden Gems",
            description = "A peaceful hillock behind the ARAI institute. Very few tourists, mostly local walkers. Spectacular city view with zero crowds. Great for bird watching.",
            image = "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?q=80&w=600&auto=format&fit=crop",
            rating = 4.6,
            latitude = 18.5147,
            longitude = 73.8184,
            address = "Kothrud, Pune",
            tags = listOf("Nature", "Quiet", "Local"),
            reviews = 2000,
            visitDuration = "1.5 hours",
            location = "Kothrud",
            timings = "24 Hours",
            entryFee = "Free",
            localName = "एआरएआय टेकडी",
            bestTime = "Sunrise or sunset"
        ),
        Attraction(
            id = 30,
            name = "Bedkar Misal",
            category = "Food",
            description = "A humble spot serving the spiciest and most authentic Misal Pav in Pune. A favorite for students seeking a budget-friendly but explosive flavor profile.",
            image = "https://images.unsplash.com/photo-1555529733-0e67056058e1?q=80&w=600&auto=format&fit=crop",
            rating = 4.7,
            latitude = 18.5164,
            longitude = 73.8540,
            address = "Shaniwar Peth, Pune",
            tags = listOf("Spicy", "Authentic", "Budget"),
            reviews = 15000,
            visitDuration = "45 mins",
            location = "Central Pune",
            timings = "8:30 AM - 1:30 PM",
            entryFee = "₹100 - ₹200",
            localName = "बेडकर मिसळ",
            bestTime = "Early morning breakfast",
            isVerified = true
        ),
        Attraction(
            id = 31,
            name = "FC Road Wall Katta",
            category = "Katta Culture",
            description = "The defining 'Katta' experience. Not a shop, but a legacy. Generations of Pune youth have sat on these low walls of FC Road to talk life, politics, and dreams over a ₹10 tea.",
            image = "https://images.unsplash.com/photo-1544073359-54313f890f84?q=80&w=600&auto=format&fit=crop",
            rating = 4.9,
            latitude = 18.5245,
            longitude = 73.8407,
            address = "FC Road, Shivajinagar",
            tags = listOf("Katta", "Authentic", "Free"),
            reviews = 100000,
            visitDuration = "2 hours",
            location = "Shivajinagar",
            timings = "All Day",
            entryFee = "Free",
            localName = "एफसी रोड कट्टा",
            isVerified = true
        ),
        Attraction(
            id = 32,
            name = "Deccan Katta",
            category = "Katta Culture",
            description = "A cultural hub near Deccan Gymkhana where theatre artists, writers, and students meet. It's the pulse of Pune’s intellectual 'Katta' culture.",
            image = "https://images.pexels.com/photos/6146978/pexels-photo-6146978.jpeg?auto=compress&cs=tinysrgb&w=600", // Representative Indian Katta vibe
            rating = 4.7,
            latitude = 18.5173,
            longitude = 73.8415,
            address = "Deccan Gymkhana, Pune",
            tags = listOf("Intellectual", "Arts", "Culture"),
            reviews = 15000,
            visitDuration = "1 hour",
            location = "Deccan",
            timings = "All Day",
            entryFee = "Free",
            localName = "डेक्कन कट्टा",
            isVerified = true
        )
    )
}
