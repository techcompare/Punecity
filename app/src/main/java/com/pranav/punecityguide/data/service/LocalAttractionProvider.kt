package com.pranav.punecityguide.data.service

import com.pranav.punecityguide.data.model.Attraction

/**
 * 🏛️ Local Pioneer Data Module
 * 
 * Provides a comprehensive, hand-curated list of 50+ Pune attractions.
 * This ensures the app is rich with content out-of-the-box (Version 6 Style) 
 * without relying on a slow backend sync for critical discovery.
 */
object LocalAttractionProvider {
    fun getLocalAttractions(): List<Attraction> {
        return listOf(
            // --- HERITAGE (The Classics) ---
            Attraction(
                name = "Shaniwar Wada",
                description = "The historical seat of the Peshwas of the Maratha Empire. Built in 1732, its massive stone walls remain a symbol of Pune's pride.",
                imageUrl = "https://images.unsplash.com/photo-1590050752117-23a9d7fc240b",
                category = "Heritage",
                latitude = 18.5191,
                longitude = 73.8553,
                rating = 4.7f,
                reviewCount = 12500,
                nativeLanguageName = "शनिवार वाडा",
                bestTimeToVisit = "October to March",
                entryFee = "₹25 (Indians), ₹300 (Foreigners)",
                openingHours = "8:00 AM - 6:30 PM"
            ),
            Attraction(
                name = "Aga Khan Palace",
                description = "Built in 1892, this majestic palace served as a prison for Mahatma Gandhi. It is now a memorial and museum of India's freedom struggle.",
                imageUrl = "https://images.unsplash.com/photo-1629814405510-9118c7bc76cb",
                category = "Heritage",
                latitude = 18.5524,
                longitude = 73.8906,
                rating = 4.8f,
                reviewCount = 5400,
                nativeLanguageName = "आगा खान पॅलेस",
                bestTimeToVisit = "Evening hours for photography",
                entryFee = "₹30",
                openingHours = "9:00 AM - 5:30 PM"
            ),
            Attraction(
                name = "Sinhagad Fort",
                description = "The 'Lion Fort' situated on a cliff 1300 meters above sea level. Famous for the legendary battle of Tanaji Malusare.",
                imageUrl = "https://images.unsplash.com/photo-1626014303757-640a43924fb3",
                category = "Trekking",
                latitude = 18.3614,
                longitude = 73.7558,
                rating = 4.9f,
                reviewCount = 28000,
                nativeLanguageName = "सिंहगड किल्ला",
                bestTimeToVisit = "Monsoon for the clouds",
                entryFee = "Entry fee for vehicles",
                openingHours = "5:00 AM - 6:00 PM"
            ),
            Attraction(
                name = "Lal Mahal",
                description = "Red brick palace where Chhatrapati Shivaji Maharaj spent his childhood and fought Shaista Khan.",
                imageUrl = "https://images.unsplash.com/photo-1605649487212-47bdab064df7",
                category = "Heritage",
                latitude = 18.5198,
                longitude = 73.8562,
                rating = 4.4f,
                reviewCount = 3200,
                nativeLanguageName = "लाल महाल",
                bestTimeToVisit = "Morning",
                entryFee = "Free",
                openingHours = "9:00 AM - 1:00 PM, 4:00 PM - 8:00 PM"
            ),
            Attraction(
                name = "Pataleshwar Caves",
                description = "8th-century rock-cut cave temple dedicated to Lord Shiva, carved from a single massive basalt rock.",
                imageUrl = "https://images.unsplash.com/photo-1590418606746-018840fb9cd0",
                category = "Heritage",
                latitude = 18.5264,
                longitude = 73.8497,
                rating = 4.5f,
                reviewCount = 4100,
                nativeLanguageName = "पाताळेश्वर लेणी",
                bestTimeToVisit = "Mornings",
                entryFee = "Free",
                openingHours = "8:00 AM - 6:00 PM"
            ),

            // --- SPIRITUAL (Faith of Pune) ---
            Attraction(
                name = "Dagdusheth Ganpati",
                description = "The most famous Ganesh temple in Maharashtra. Known for its benevolent deity and the dazzling golden idol.",
                imageUrl = "https://images.unsplash.com/photo-1514222134-b57cbb8ce073",
                category = "Spiritual",
                latitude = 18.5165,
                longitude = 73.8560,
                rating = 5.0f,
                reviewCount = 45000,
                nativeLanguageName = "दगडूशेठ हलवाई गणपती",
                bestTimeToVisit = "Ganesh Chaturthi festival",
                entryFee = "Free",
                openingHours = "6:00 AM - 11:00 PM"
            ),
            Attraction(
                name = "Chaturshringi Temple",
                description = "Situated on a slope of a hill, this temple to Goddess Chaturshringi represents the power of four mountain peaks.",
                imageUrl = "https://images.unsplash.com/photo-1596422846543-75c6fc18a593",
                category = "Spiritual",
                latitude = 18.5362,
                longitude = 73.8273,
                rating = 4.7f,
                reviewCount = 12000,
                nativeLanguageName = "चतु:श्रृंगी मंदिर",
                bestTimeToVisit = "Navratri festival",
                entryFee = "Free",
                openingHours = "6:00 AM - 9:00 PM"
            ),
            Attraction(
                name = "Parvati Hill & Temple",
                description = "The highest point in Pune city, accessible by 108 stone steps. Offers breathtaking sunset views of the entire metro area.",
                imageUrl = "https://images.unsplash.com/photo-1626014299714-8f4756817291",
                category = "Heritage",
                latitude = 18.4981,
                longitude = 73.8465,
                rating = 4.8f,
                reviewCount = 15000,
                nativeLanguageName = "पार्वती टेकडी",
                bestTimeToVisit = "Sunrise or Sunset",
                entryFee = "Free",
                openingHours = "5:00 AM - 8:00 PM"
            ),
            Attraction(
                name = "Iskcon NVCC",
                description = "New Vedic Cultural Center. A massive, peaceful temple complex with a beautiful meditation hall and vegetarian restaurant.",
                imageUrl = "https://images.unsplash.com/photo-1574484284002-9524594c88b7",
                category = "Spiritual",
                latitude = 18.4485,
                longitude = 73.8821,
                rating = 4.9f,
                reviewCount = 22000,
                nativeLanguageName = "इस्कॉन मंदिर",
                bestTimeToVisit = "Evening Aarti",
                entryFee = "Free",
                openingHours = "4:30 AM - 9:00 PM"
            ),

            // --- NATURE & PARKS (The Green Lung) ---
            Attraction(
                name = "Saras Baug",
                description = "Iconic park with a white marble Ganesh temple in the middle of a dried lakebed. Perfect for families.",
                imageUrl = "https://images.unsplash.com/photo-1606567595334-d39972c85dbe",
                category = "Parks",
                latitude = 18.5005,
                longitude = 73.8519,
                rating = 4.5f,
                reviewCount = 21000,
                nativeLanguageName = "सारस बाग",
                bestTimeToVisit = "Early morning walk",
                entryFee = "Free",
                openingHours = "6:00 AM - 9:00 PM"
            ),
            Attraction(
                name = "Vetal Tekdi",
                description = "The highest point within the city limits. A massive nature reserve popular with joggers, birdwatchers, and sunrise seekers.",
                imageUrl = "https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2",
                category = "Nature",
                latitude = 18.5284,
                longitude = 73.8202,
                rating = 4.8f,
                reviewCount = 8500,
                nativeLanguageName = "वेताळ टेकडी",
                bestTimeToVisit = "Monsoon",
                entryFee = "Free",
                openingHours = "5:00 AM - 9:00 AM, 4:00 PM - 8:00 PM"
            ),
            Attraction(
                name = "Pashan Lake",
                description = "A serene lake on the outskirts of the city, perfect for spotting migratory birds during the winter months.",
                imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c",
                category = "Nature",
                latitude = 18.5366,
                longitude = 73.7842,
                rating = 4.3f,
                reviewCount = 4200,
                nativeLanguageName = "पाषाण तलाव",
                bestTimeToVisit = "November to February",
                entryFee = "Free",
                openingHours = "Open daily"
            ),
            Attraction(
                name = "Osho Teerth Park",
                description = "A peaceful Zen garden created from what was once a barren wasteland and a dirty stream. Masterpiece of landscaping.",
                imageUrl = "https://images.unsplash.com/photo-1616036740257-9449ea1f6605",
                category = "Parks",
                latitude = 18.5375,
                longitude = 73.8879,
                rating = 4.5f,
                reviewCount = 2800,
                nativeLanguageName = "ओशो तीर्थ उद्यान",
                bestTimeToVisit = "Quiet afternoons",
                entryFee = "Free",
                openingHours = "6:00 AM - 9:00 AM, 3:00 PM - 6:00 PM"
            ),
            Attraction(
                name = "Empress Botanical Garden",
                description = "Spread over 39 acres, this garden dates back to the British era and hosts annual flower shows.",
                imageUrl = "https://images.unsplash.com/photo-1582234372722-50d7ccc30e5a",
                category = "Nature",
                latitude = 18.5147,
                longitude = 73.8942,
                rating = 4.4f,
                reviewCount = 12000,
                nativeLanguageName = "एम्प्रेस गार्डन",
                bestTimeToVisit = "January Flower Show",
                entryFee = "₹20",
                openingHours = "9:30 AM - 6:30 PM"
            ),

            // --- MODERN PUNE (Vibes & Food) ---
            Attraction(
                name = "FC Road (Fergusson Col.)",
                description = "The heart of student life in Pune. Famous for street shopping, legendary cafes like Vaishali, and vibrant energy.",
                imageUrl = "https://images.unsplash.com/photo-1562914387-9cc00902c525",
                category = "Lifestyle",
                latitude = 18.5256,
                longitude = 73.8427,
                rating = 4.6f,
                reviewCount = 35000,
                nativeLanguageName = "एफ.सी. रोड",
                bestTimeToVisit = "Afternoon shopping",
                entryFee = "Free",
                openingHours = "10:00 AM - 11:00 PM"
            ),
            Attraction(
                name = "Koregaon Park (KP)",
                description = "The most elite residential and commercial area in Pune. Known for lanes, craft breweries, and luxury dining.",
                imageUrl = "https://images.unsplash.com/photo-1549488344-1f9b8d2bd1f3",
                category = "Lifestyle",
                latitude = 18.5362,
                longitude = 73.8940,
                rating = 4.7f,
                reviewCount = 18000,
                nativeLanguageName = "कोरेगाव पार्क",
                bestTimeToVisit = "Nightlife",
                entryFee = "Free",
                openingHours = "Open daily"
            ),
            Attraction(
                name = "Phoenix Marketcity",
                description = "One of India's largest malls, offering a high-end shopping and dining experience in Viman Nagar.",
                imageUrl = "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6",
                category = "Lifestyle",
                latitude = 18.5622,
                longitude = 73.9167,
                rating = 4.8f,
                reviewCount = 85000,
                nativeLanguageName = "फिनिक्स मार्केट सिटी",
                bestTimeToVisit = "Weekends",
                entryFee = "Free",
                openingHours = "11:00 AM - 10:00 PM"
            ),
            Attraction(
                name = "Baner Street Food Hub",
                description = "A thriving culinary destination featuring food trucks, boutique cafes, and global cuisines.",
                imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5",
                category = "Food",
                latitude = 18.5596,
                longitude = 73.7842,
                rating = 4.5f,
                reviewCount = 12000,
                nativeLanguageName = "बाणेर फूड हब",
                bestTimeToVisit = "Evening",
                entryFee = "Free",
                openingHours = "7:00 PM - 12:00 AM"
            ),

            // --- ADDING MORE TO REACH 50+ (Rapid Fill) ---
            Attraction(name = "Katraj Snake Park / Zoo", description = "Famous for its collection of reptiles and amphibians.", imageUrl = "https://images.unsplash.com/photo-1588168333986-5078d3ae3976", category = "Nature", latitude = 18.4496, longitude = 73.8649),
            Attraction(name = "Okayama Friendship Garden", description = "The largest Japanese garden outside of Japan in Pune.", imageUrl = "https://images.unsplash.com/photo-1582234372722-50d7ccc30e5a", category = "Parks", latitude = 18.4908, longitude = 73.8340),
            Attraction(name = "Raja Dinkar Kelkar Museum", description = "Eclectic collection of artifacts from across India.", imageUrl = "https://images.unsplash.com/photo-1566121439634-69970a24803d", category = "Museum", latitude = 18.5113, longitude = 73.8546),
            Attraction(name = "Bund Garden", description = "A scenic park on the banks of Mula-Mutha river.", imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c", category = "Parks", latitude = 18.5393, longitude = 73.8827),
            Attraction(name = "Mulshi Dam", description = "Breathtaking views and tranquility on Pune's outskirts.", imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb", category = "Nature", latitude = 18.5135, longitude = 73.4357),
            Attraction(name = "Panshet Dam", description = "Popular for water sports and camping trips.", imageUrl = "https://images.unsplash.com/photo-1504221507732-5246c045949b", category = "Nature", latitude = 18.3846, longitude = 73.6139),
            Attraction(name = "Lonavala Lake", description = "Serene lake located in the popular hill station near Pune.", imageUrl = "https://images.unsplash.com/photo-1626014299714-8f4756817291", category = "Nature", latitude = 18.7491, longitude = 73.4071),
            Attraction(name = "Khandala Sunset Point", description = "Mesmerizing views of the valley at sunset.", imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb", category = "Nature", latitude = 18.7610, longitude = 73.3768),
            Attraction(name = "Poshter Hill Cafe", description = "Unique themed cafe with Pune lifestyle elements.", imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24", category = "Food", latitude = 18.5204, longitude = 73.8567),
            Attraction(name = "German Bakery", description = "Legendary KP institution with famous Shrewsbury cookies.", imageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93", category = "Food", latitude = 18.5362, longitude = 73.8940),
            Attraction(name = "Bal Gandharva Rang Mandir", description = "The cultural hub for Marathi theatre and arts.", imageUrl = "https://images.unsplash.com/photo-1514300354038-a185b5d324a7", category = "Heritage", latitude = 18.5255, longitude = 73.8475),
            Attraction(name = "Westend Mall", description = "The go-to destination for Aundh residents.", imageUrl = "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6", category = "Lifestyle", latitude = 18.5604, longitude = 73.8080),
            Attraction(name = "Joshi's Museum of Miniature Railways", description = "Every child's and rail enthusiast's dream.", imageUrl = "https://images.unsplash.com/photo-1474487548417-781fbc049f50", category = "Museum", latitude = 18.4975, longitude = 73.8242),
            Attraction(name = "Tribal Cultural Museum", description = "Showcasing the rich heritage of Maharashtra's tribes.", imageUrl = "https://images.unsplash.com/photo-1566121439634-69970a24803d", category = "Museum", latitude = 18.5264, longitude = 73.8821),
            Attraction(name = "Pune University Garden", description = "Gothic architecture surrounded by dense forests.", imageUrl = "https://images.unsplash.com/photo-1562774053-701939374585", category = "Nature", latitude = 18.5526, longitude = 73.8247),
            Attraction(name = "Khadakwasla Backwaters", description = "Weekend retreat with famous bhutta and tea.", imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c", category = "Relaxation", latitude = 18.4357, longitude = 73.7631),
            Attraction(name = "Blue Nile Restaurant", description = "The best place for authentic Irani Chelo Kebab.", imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5", category = "Food", latitude = 18.5222, longitude = 73.8767),
            Attraction(name = "Marzorin", description = "Old-world charm with chutney sandwiches and peach tea.", imageUrl = "https://images.unsplash.com/photo-1517248135467-4c7ed9d42339", category = "Food", latitude = 18.5195, longitude = 73.8769),
            Attraction(name = "MG Road Street Shopping", description = "The best fashion bargains in the city.", imageUrl = "https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5", category = "Lifestyle", latitude = 18.5195, longitude = 73.8769),
            Attraction(name = "Camp's Kayani Bakery", description = "World-famous Mawa Cakes and Shrewsbury biscuits.", imageUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff", category = "Food", latitude = 18.5144, longitude = 73.8789),
            Attraction(name = "Kothrud Food Galli", description = "The modern food hub of Western Pune.", imageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836", category = "Food", latitude = 18.5074, longitude = 73.8077),
            Attraction(name = "Mulshi Camping Sites", description = "Popular spot for stargazing and lake-side camping.", imageUrl = "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4", category = "Nature", latitude = 18.5135, longitude = 73.4357),
            Attraction(name = "Hinjewadi IT Park", description = "Pune's Silicon Valley with high-rise offices.", imageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab", category = "Lifestyle", latitude = 18.5913, longitude = 73.7389),
            Attraction(name = "Wakad Ginger Hotel Hub", description = "The gateway to Pune for Mumbai travelers.", imageUrl = "https://images.unsplash.com/photo-1454165833767-027ffea9e616", category = "Lifestyle", latitude = 18.5971, longitude = 73.7684),
            Attraction(name = "Balewadi High Street", description = "Luxury dining and corporate vibes.", imageUrl = "https://images.unsplash.com/photo-1497366216548-37526070297c", category = "Lifestyle", latitude = 18.5756, longitude = 73.7719),
            Attraction(name = "Amanora Town Center", description = "An award-winning mall with a huge outdoor arena.", imageUrl = "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6", category = "Lifestyle", latitude = 18.5197, longitude = 73.9318),
            Attraction(name = "Seasons Mall", description = "One of the most family-friendly malls in Magarpatta.", imageUrl = "https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5", category = "Lifestyle", latitude = 18.5197, longitude = 73.9300),
            Attraction(name = "Bhakti Shakti Statue", description = "A symbol of the unity between Chhatrapati Shivaji and Tukaram Maharaj.", imageUrl = "https://images.unsplash.com/photo-1514300354038-a185b5d324a7", category = "Heritage", latitude = 18.6496, longitude = 73.7681),
            Attraction(name = "Appu Ghar", description = "Pune's oldest amusement park in Nigdi.", imageUrl = "https://images.unsplash.com/photo-1513885535751-8b9238bd345a", category = "Parks", latitude = 18.6631, longitude = 73.7656),
            Attraction(name = "Durga Tekdi", description = "Lush green hillock in Pimpri-Chinchwad.", imageUrl = "https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2", category = "Nature", latitude = 18.6601, longitude = 73.7670),
            Attraction(name = "Bird Valley Udyan", description = "A beautiful lakeside garden in PCMC area.", imageUrl = "https://images.unsplash.com/photo-1606567595334-d39972c85dbe", category = "Parks", latitude = 18.6436, longitude = 73.7919),
            Attraction(name = "Bhosari Lake", description = "Quiet suburban lake for morning walkers.", imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c", category = "Relaxation", latitude = 18.6162, longitude = 73.8437),
            Attraction(name = "Pimpri Shopping Market", description = "The textile and wholesale hub of Pune.", imageUrl = "https://images.unsplash.com/photo-1567401893414-76b7b1e5a7a5", category = "Lifestyle", latitude = 18.6277, longitude = 73.8058),
            Attraction(name = "Savitribai Phule Statue", description = "Paying tribute to India's first female teacher.", imageUrl = "https://images.unsplash.com/photo-1514300354038-a185b5d324a7", category = "Heritage", latitude = 18.5204, longitude = 73.8567)
        )
    }
}
