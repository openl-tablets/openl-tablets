package org.openl.generated

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

interface OpenAPIService {
    @POST
    @Path("/SpecialFeaturesLoading")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal SpecialFeaturesLoading(String classCode)

    @POST
    @Path("/MSDcontents")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal MSDcontents(String msdLevel)

    @POST
    @Path("/OptionType")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal OptionType(String optionType)

    @POST
    @Path("/ExcessPremiumMultiplierContents")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal ExcessPremiumMultiplierContents(String classCode)

    @POST
    @Path("/RatingArea1")
    @Consumes("text/plain")
    @Produces("text/plain")
    String RatingArea1(String postCode)

    @POST
    @Path("/ContentsRIPaymentFrequency")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal ContentsRIPaymentFrequency(String paymentOption)

    @POST
    @Path("/HouseArea")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal HouseArea(Integer area)

    @POST
    @Path("/MSD")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal MSD(String msdLevel)

    @POST
    @Path("/BuidingTypeHouse")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal BuidingTypeHouse(String buildingType)

    @POST
    @Path("/Slope")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal Slope(String slope)

    @POST
    @Path("/CalculateLimiting")
    @Consumes("application/json")
    @Produces("application/json")
    Boolean CalculateLimiting(BigDecimal paidLastYear, String transaction)

    @POST
    @Path("/NumberOfDwellings")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal NumberOfDwellings(Integer noDwellings)

    @POST
    @Path("/ContentsPaymentFrequency")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal ContentsPaymentFrequency(String paymentOption)

    @POST
    @Path("/Storey")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal Storey(Integer storey)

    @POST
    @Path("/MatchingCarpets")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal MatchingCarpets(String ratingArea)

    @POST
    @Path("/ExcessPremiumMultiplier")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal ExcessPremiumMultiplier(String classCode)

    @POST
    @Path("/HousePaymentFrequency")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal HousePaymentFrequency(String paymentOption)

    @POST
    @Path("/Standard")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal Standard(String standard)

    @POST
    @Path("/BuildingTypeContents")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal BuildingTypeContents(String buildingType)

    @POST
    @Path("/RemoveZero")
    @Consumes("application/json")
    @Produces("application/json")
    BigDecimal[] RemoveZero(BigDecimal[] array)

    @POST
    @Path("/AdjContents")
    @Consumes("text/plain")
    @Produces("text/plain")
    BigDecimal AdjContents(String parameter)
}
