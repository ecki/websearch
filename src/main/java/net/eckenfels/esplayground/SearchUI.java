package net.eckenfels.esplayground;

import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/* 
 * UI class is the starting point for your app. You may deploy it with VaadinServlet
 * or VaadinPortlet by giving your UI class name a parameter. When you browse to your
 * app a web page showing your UI is automatically generated. Or you may choose to 
 * embed your UI to an existing web page. 
 */
@Title("Websearch")
public class SearchUI extends UI
{
    private static final long serialVersionUID = 1L;

    /* User interface components are stored in session. */
	private TextField searchField = new TextField();
	private Button searchButton = new Button("Search");

    private VerticalLayout leftLayout;

    private VerticalLayout resultLayout;

	/*
	 * After UI class is created, init() is executed. You should build and wire
	 * up your user interface here.
	 */
	protected void init(VaadinRequest request) {
	    StartServer.startES();
		initLayout();
		initSearch();
		StartServer.populate();
		
	}

	/*
	 * In this example layouts are programmed in Java. You may choose use a
	 * visual editor, CSS or HTML templates for layout instead.
	 */
	private void initLayout()
	{
		/* Root of the user interface component tree is set */
		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		setContent(splitPanel);

	    leftLayout = new VerticalLayout();
        leftLayout.setSpacing(true);
        splitPanel.addComponent(leftLayout);
        
        leftLayout.addComponent(new Label("<b>Please Wait</b>", ContentMode.HTML));

        /* Build the component tree */
        VerticalLayout rightLayout = new VerticalLayout();
        HorizontalLayout box = new HorizontalLayout();
        box.addComponent(searchField);
        box.addComponent(searchButton);
        rightLayout.addComponent(box);
        rightLayout.setHeight(SIZE_UNDEFINED, Unit.PIXELS);
        
        resultLayout = new VerticalLayout();
        resultLayout.setHeight("100%");
        rightLayout.addComponent(resultLayout);
        rightLayout.setExpandRatio(resultLayout, 1.0f);
        
		splitPanel.addComponent(rightLayout);

		/* Set the contents in the left of the split panel to use all the space */
		//rightLayout.setSizeFull();

		/*
		 * On the left side, expand the size of the contactList so that it uses
		 * all the space left after from bottomLeftLayout
		 */
		//leftLayout.setExpandRatio(contactList, 1);
		//contactList.setSizeFull();
	}


	private void initSearch()
	{
		searchField.setInputPrompt("Search");

		/*
		 * Granularity for sending events over the wire can be controlled. By
		 * default simple changes like writing a text in TextField are sent to
		 * server with the next Ajax call. You can set your component to be
		 * immediate to send the changes to server immediately after focus
		 * leaves the field. Here we choose to send the text over the wire as
		 * soon as user stops writing for a moment.
		 */
		searchField.setTextChangeEventMode(TextChangeEventMode.LAZY);

		/*
		 * When the event happens, we handle it in the anonymous inner class.
		 * You may choose to use separate controllers (in MVC) or presenters (in
		 * MVP) instead. In the end, the preferred application architecture is
		 * up to you.
		 */
		/*searchField.addTextChangeListener(new TextChangeListener() {
			public void textChange(final TextChangeEvent event) {
			    System.out.println("Enter: " + event.getText());
			}
		});*/
		
		searchButton.addClickListener(new com.vaadin.ui.Button.ClickListener() {
    		    public void buttonClick(ClickEvent event)
    		    {
                    System.out.println("Enter: '" + searchField.getValue()+"'");
    		        StartServer.searchFor(searchField.getValue(), leftLayout, resultLayout);
    		    }
        });
	}

}
