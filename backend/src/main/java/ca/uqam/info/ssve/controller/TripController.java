/**
 * The TripController class uses a Rest Api to retrieve a request
 * with a set of trip data sent by the front end
 *
 */
package java.ca.uqam.info.ssve.controller;

import java.ca.uqam.info.ssve.model.TripNeeds;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/tripneeds")
public class TripController {

	@PostMapping("")
	public @ResponseBody TripNeeds postTrip(@RequestBody TripNeeds tripNeeds) {
		System.out.println(tripNeeds.toString());
		return tripNeeds;
	}
}