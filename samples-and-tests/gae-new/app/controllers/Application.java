package controllers;

import models.Message;
import play.mvc.Controller;

public class Application extends Controller {

	public static void index() {
		render();
	}

	public static void displaySimpleArgs() {
	    String country = "France";
        String capital = "Paris";
		render(country, capital);
	}

	public static void displayModelArgs() {
		Message info = new Message("Australia", "Canberra");
		render(info);
	}
}