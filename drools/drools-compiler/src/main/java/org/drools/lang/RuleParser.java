// $ANTLR 3.0ea8 D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g 2006-08-30 13:06:30

	package org.drools.lang;
	import java.util.List;
	import java.util.ArrayList;
	import java.util.Iterator;
	import java.util.Map;	
	import java.util.HashMap;	
	import java.util.StringTokenizer;
	import org.drools.lang.descr.*;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class RuleParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "EOL", "ID", "INT", "BOOL", "STRING", "FLOAT", "MISC", "WS", "SH_STYLE_SINGLE_LINE_COMMENT", "C_STYLE_SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT", "\';\'", "\'package\'", "\'import\'", "\'.\'", "\'.*\'", "\'expander\'", "\'global\'", "\'function\'", "\'(\'", "\',\'", "\')\'", "\'{\'", "\'}\'", "\'query\'", "\'end\'", "\'template\'", "\'rule\'", "\'when\'", "\':\'", "\'then\'", "\'attributes\'", "\'salience\'", "\'no-loop\'", "\'auto-focus\'", "\'activation-group\'", "\'agenda-group\'", "\'duration\'", "\'from\'", "\'null\'", "\'=>\'", "\'or\'", "\'||\'", "\'&\'", "\'|\'", "\'->\'", "\'and\'", "\'&&\'", "\'exists\'", "\'not\'", "\'eval\'", "\'[\'", "\']\'", "\'use\'", "\'==\'", "\'=\'", "\'>\'", "\'>=\'", "\'<\'", "\'<=\'", "\'!=\'", "\'contains\'", "\'matches\'", "\'excludes\'"
    };
    public static final int BOOL=7;
    public static final int INT=6;
    public static final int WS=11;
    public static final int EOF=-1;
    public static final int MISC=10;
    public static final int STRING=8;
    public static final int EOL=4;
    public static final int FLOAT=9;
    public static final int SH_STYLE_SINGLE_LINE_COMMENT=12;
    public static final int MULTI_LINE_COMMENT=14;
    public static final int C_STYLE_SINGLE_LINE_COMMENT=13;
    public static final int ID=5;
        public RuleParser(TokenStream input) {
            super(input);
        }
        

    public String[] getTokenNames() { return tokenNames; }

    
    	private ExpanderResolver expanderResolver;
    	private Expander expander;
    	private boolean expanderDebug = false;
    	private PackageDescr packageDescr;
    	private List errors = new ArrayList();
    	private String source = "unknown";
    	private int lineOffset = 0;
    	private DescrFactory factory = new DescrFactory();
    	
    	
    	private boolean parserDebug = false;
    	
    	public void setParserDebug(boolean parserDebug) {
    		this.parserDebug = parserDebug;
    	}
    	
    	public void debug(String message) {
    		if ( parserDebug ) 
    			System.err.println( "drl parser: " + message );
    	}
    	
    	public void setSource(String source) {
    		this.source = source;
    	}
    	
    	public DescrFactory getFactory() {
    		return factory;
    	}	
    
    	/**
    	 * This may be set to enable debuggin of DSLs/expanders.
    	 * If set to true, expander stuff will be sent to the Std out.
    	 */	
    	public void setExpanderDebug(boolean status) {
    		expanderDebug = status;
    	}
    	public String getSource() {
    		return this.source;
    	}
    	
    	public PackageDescr getPackageDescr() {
    		return packageDescr;
    	}
    	
    	private int offset(int line) {
    		return line + lineOffset;
    	}
    	
    	/**
    	 * This will set the offset to record when reparsing. Normally is zero of course 
    	 */
    	public void setLineOffset(int i) {
    	 	this.lineOffset = i;
    	}
    	
    	public void setExpanderResolver(ExpanderResolver expanderResolver) {
    		this.expanderResolver = expanderResolver;
    	}
    	
    	public ExpanderResolver getExpanderResolver() {
    		return expanderResolver;
    	}
    	
    	/** Expand the LHS */
    	private String runWhenExpander(String text, int line) throws RecognitionException {
    		String expanded = text.trim();
    		if (expanded.startsWith(">")) {
    			expanded = expanded.substring(1);  //escape !!
    		} else {
    			try {
    				expanded = expander.expand( "when", text );			
    			} catch (Exception e) {
    				this.errors.add(new ExpanderException("Unable to expand: " + text + ". Due to " + e.getMessage(), line));
    				return "";
    			}
    		}
    		if (expanderDebug) {
    			System.out.println("Expanding LHS: " + text + " ----> " + expanded + " --> from line: " + line);
    		}
    		return expanded;			
    	}
    	
        	/** This will apply a list of constraints to an LHS block */
        	private String applyConstraints(List constraints, String block) {
        		//apply the constraints as a comma seperated list inside the previous block
        		//the block will end in something like "foo()" and the constraint patterns will be put in the ()
        		if (constraints == null) {
        			return block;
        		}
        		StringBuffer list = new StringBuffer();    		
        		for (Iterator iter = constraints.iterator(); iter.hasNext();) {
    				String con = (String) iter.next();
    				list.append("\n\t\t");
    				list.append(con);
    				if (iter.hasNext()) {
    					list.append(",");					
    				}			
    			}
        		if (block.endsWith("()")) {
        			return block.substring(0, block.length() - 2) + "(" + list.toString() + ")";
        		} else {
        			return block + "(" + list.toString() + ")";
        		}
        	}  
        	
            	/** Reparse the results of the expansion */
        	private void reparseLhs(String text, AndDescr descrs) throws RecognitionException {
        		CharStream charStream = new ANTLRStringStream( text  + " \n  then"); //need to then so it knows when to end... werd...
        		RuleParserLexer lexer = new RuleParserLexer( charStream );
        		TokenStream tokenStream = new CommonTokenStream( lexer );
        		RuleParser parser = new RuleParser( tokenStream );
        		parser.setLineOffset( descrs.getLine() );
        		parser.normal_lhs_block(descrs);
                
                    if (parser.hasErrors()) {
        			this.errors.addAll(parser.getErrors());
        		}
    		if (expanderDebug) {
    			System.out.println("Reparsing LHS: " + text + " --> successful:" + !parser.hasErrors());
    		}    		
        		
        	}
    	
    	/** Expand a line on the RHS */
    	private String runThenExpander(String text, int startLine) {
    		//System.err.println( "expand THEN [" + text + "]" );
    		StringTokenizer lines = new StringTokenizer( text, "\n\r" );
    
    		StringBuffer expanded = new StringBuffer();
    		
    		String eol = System.getProperty( "line.separator" );
    				
    		while ( lines.hasMoreTokens() ) {
    			startLine++;
    			String line = lines.nextToken();
    			line = line.trim();
    			if ( line.length() > 0 ) {
    				if ( line.startsWith( ">" ) ) {
    					expanded.append( line.substring( 1 ) );
    					expanded.append( eol );
    				} else {
    					try {
    						expanded.append( expander.expand( "then", line ) );
    						expanded.append( eol );
    					} catch (Exception e) {
    						this.errors.add(new ExpanderException("Unable to expand: " + line + ". Due to " + e.getMessage(), startLine));			
    					}
    				}
    			}
    		}
    		
    		if (expanderDebug) {
    			System.out.println("Expanding RHS: " + text + " ----> " + expanded.toString() + " --> from line starting: " + startLine);
    		}		
    		
    		return expanded.toString();
    	}
    	
    
    	
    	private String getString(Token token) {
    		String orig = token.getText();
    		return orig.substring( 1, orig.length() -1 );
    	}
    	
    	public void reportError(RecognitionException ex) {
    	        // if we've already reported an error and have not matched a token
                    // yet successfully, don't report any errors.
                    if ( errorRecovery ) {
                            return;
                    }
                    errorRecovery = true;
    
    		ex.line = offset(ex.line); //add the offset if there is one
    		errors.add( ex ); 
    	}
         	
         	/** return the raw RecognitionException errors */
         	public List getErrors() {
         		return errors;
         	}
         	
         	/** Return a list of pretty strings summarising the errors */
         	public List getErrorMessages() {
         		List messages = new ArrayList();
     		for ( Iterator errorIter = errors.iterator() ; errorIter.hasNext() ; ) {
         	     		messages.add( createErrorMessage( (RecognitionException) errorIter.next() ) );
         	     	}
         	     	return messages;
         	}
         	
         	/** return true if any parser errors were accumulated */
         	public boolean hasErrors() {
      		return ! errors.isEmpty();
         	}
         	
         	/** This will take a RecognitionException, and create a sensible error message out of it */
         	public String createErrorMessage(RecognitionException e)
            {
    		StringBuffer message = new StringBuffer();		
                    message.append( source + ":"+e.line+":"+e.charPositionInLine+" ");
                    if ( e instanceof MismatchedTokenException ) {
                            MismatchedTokenException mte = (MismatchedTokenException)e;
                            message.append("mismatched token: "+
                                                               e.token+
                                                               "; expecting type "+
                                                               tokenNames[mte.expecting]);
                    }
                    else if ( e instanceof MismatchedTreeNodeException ) {
                            MismatchedTreeNodeException mtne = (MismatchedTreeNodeException)e;
                            message.append("mismatched tree node: "+
                                                               mtne.foundNode+
                                                               "; expecting type "+
                                                               tokenNames[mtne.expecting]);
                    }
                    else if ( e instanceof NoViableAltException ) {
                            NoViableAltException nvae = (NoViableAltException)e;
    			message.append( "Unexpected token '" + e.token.getText() + "'" );
                            /*
                            message.append("decision=<<"+nvae.grammarDecisionDescription+">>"+
                                                               " state "+nvae.stateNumber+
                                                               " (decision="+nvae.decisionNumber+
                                                               ") no viable alt; token="+
                                                               e.token);
                                                               */
                    }
                    else if ( e instanceof EarlyExitException ) {
                            EarlyExitException eee = (EarlyExitException)e;
                            message.append("required (...)+ loop (decision="+
                                                               eee.decisionNumber+
                                                               ") did not match anything; token="+
                                                               e.token);
                    }
                    else if ( e instanceof MismatchedSetException ) {
                            MismatchedSetException mse = (MismatchedSetException)e;
                            message.append("mismatched token '"+
                                                               e.token+
                                                               "' expecting set "+mse.expecting);
                    }
                    else if ( e instanceof MismatchedNotSetException ) {
                            MismatchedNotSetException mse = (MismatchedNotSetException)e;
                            message.append("mismatched token '"+
                                                               e.token+
                                                               "' expecting set "+mse.expecting);
                    }
                    else if ( e instanceof FailedPredicateException ) {
                            FailedPredicateException fpe = (FailedPredicateException)e;
                            message.append("rule "+fpe.ruleName+" failed predicate: {"+
                                                               fpe.predicateText+"}?");
                    } else if (e instanceof GeneralParseException) {
    			message.append(" " + e.getMessage());
    		}
                   	return message.toString();
            }   
            
            void checkTrailingSemicolon(String text, int line) {
            	if (text.trim().endsWith( ";" ) ) {
            		this.errors.add( new GeneralParseException( "Trailing semi-colon not allowed", offset(line) ) );
            	}
            }
          



    // $ANTLR start opt_eol
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:282:1: opt_eol : ( (';'|EOL))* ;
    public void opt_eol() throws RecognitionException {   
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:283:17: ( ( (';'|EOL))* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:283:17: ( (';'|EOL))*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:283:17: ( (';'|EOL))*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);
                if ( LA1_0==EOL ) {
                    alt1=1;
                }
                else if ( LA1_0==15 ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:283:18: (';'|EOL)
            	    {
            	    if ( input.LA(1)==EOL||input.LA(1)==15 ) {
            	        input.consume();
            	        errorRecovery=false;
            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_opt_eol41);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end opt_eol


    // $ANTLR start compilation_unit
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:286:1: compilation_unit : opt_eol prolog (r= rule | q= query | t= template | extra_statement )* ;
    public void compilation_unit() throws RecognitionException {   
        RuleDescr r = null;

        QueryDescr q = null;

        FactTemplateDescr t = null;


        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:287:17: ( opt_eol prolog (r= rule | q= query | t= template | extra_statement )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:287:17: opt_eol prolog (r= rule | q= query | t= template | extra_statement )*
            {
            following.push(FOLLOW_opt_eol_in_compilation_unit57);
            opt_eol();
            following.pop();

            following.push(FOLLOW_prolog_in_compilation_unit61);
            prolog();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:289:17: (r= rule | q= query | t= template | extra_statement )*
            loop2:
            do {
                int alt2=5;
                alt2 = dfa2.predict(input); 
                switch (alt2) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:289:25: r= rule
            	    {
            	    following.push(FOLLOW_rule_in_compilation_unit70);
            	    r=rule();
            	    following.pop();

            	    this.packageDescr.addRule( r ); 

            	    }
            	    break;
            	case 2 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:290:25: q= query
            	    {
            	    following.push(FOLLOW_query_in_compilation_unit83);
            	    q=query();
            	    following.pop();

            	    this.packageDescr.addRule( q ); 

            	    }
            	    break;
            	case 3 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:291:25: t= template
            	    {
            	    following.push(FOLLOW_template_in_compilation_unit93);
            	    t=template();
            	    following.pop();

            	    this.packageDescr.addFactTemplate ( t ); 

            	    }
            	    break;
            	case 4 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:292:25: extra_statement
            	    {
            	    following.push(FOLLOW_extra_statement_in_compilation_unit101);
            	    extra_statement();
            	    following.pop();


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end compilation_unit


    // $ANTLR start prolog
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:296:1: prolog : opt_eol (name= package_statement )? ( extra_statement | expander )* opt_eol ;
    public void prolog() throws RecognitionException {   
        String name = null;


        
        		String packageName = "";
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:300:17: ( opt_eol (name= package_statement )? ( extra_statement | expander )* opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:300:17: opt_eol (name= package_statement )? ( extra_statement | expander )* opt_eol
            {
            following.push(FOLLOW_opt_eol_in_prolog125);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:301:17: (name= package_statement )?
            int alt3=2;
            int LA3_0 = input.LA(1);
            if ( LA3_0==16 ) {
                alt3=1;
            }
            else if ( LA3_0==-1||LA3_0==EOL||LA3_0==15||LA3_0==17||(LA3_0>=20 && LA3_0<=22)||LA3_0==28||(LA3_0>=30 && LA3_0<=31) ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("301:17: (name= package_statement )?", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:301:19: name= package_statement
                    {
                    following.push(FOLLOW_package_statement_in_prolog133);
                    name=package_statement();
                    following.pop();

                     packageName = name; 

                    }
                    break;

            }

             
            			this.packageDescr = new PackageDescr( name ); 
            		
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:305:17: ( extra_statement | expander )*
            loop4:
            do {
                int alt4=3;
                alt4 = dfa4.predict(input); 
                switch (alt4) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:305:25: extra_statement
            	    {
            	    following.push(FOLLOW_extra_statement_in_prolog148);
            	    extra_statement();
            	    following.pop();


            	    }
            	    break;
            	case 2 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:306:25: expander
            	    {
            	    following.push(FOLLOW_expander_in_prolog154);
            	    expander();
            	    following.pop();


            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);

            following.push(FOLLOW_opt_eol_in_prolog166);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end prolog


    // $ANTLR start package_statement
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:312:1: package_statement returns [String packageName] : 'package' opt_eol name= dotted_name ( ';' )? opt_eol ;
    public String package_statement() throws RecognitionException {   
        String packageName;
        String name = null;


        
        		packageName = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:317:17: ( 'package' opt_eol name= dotted_name ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:317:17: 'package' opt_eol name= dotted_name ( ';' )? opt_eol
            {
            match(input,16,FOLLOW_16_in_package_statement190); 
            following.push(FOLLOW_opt_eol_in_package_statement192);
            opt_eol();
            following.pop();

            following.push(FOLLOW_dotted_name_in_package_statement196);
            name=dotted_name();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:317:52: ( ';' )?
            int alt5=2;
            int LA5_0 = input.LA(1);
            if ( LA5_0==15 ) {
                alt5=1;
            }
            else if ( LA5_0==-1||LA5_0==EOL||LA5_0==17||(LA5_0>=20 && LA5_0<=22)||LA5_0==28||(LA5_0>=30 && LA5_0<=31) ) {
                alt5=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("317:52: ( \';\' )?", 5, 0, input);

                throw nvae;
            }
            switch (alt5) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:317:52: ';'
                    {
                    match(input,15,FOLLOW_15_in_package_statement198); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_package_statement201);
            opt_eol();
            following.pop();

            
            			packageName = name;
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return packageName;
    }
    // $ANTLR end package_statement


    // $ANTLR start import_statement
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:323:1: import_statement : 'import' opt_eol name= import_name ( ';' )? opt_eol ;
    public void import_statement() throws RecognitionException {   
        String name = null;


        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:324:17: ( 'import' opt_eol name= import_name ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:324:17: 'import' opt_eol name= import_name ( ';' )? opt_eol
            {
            match(input,17,FOLLOW_17_in_import_statement217); 
            following.push(FOLLOW_opt_eol_in_import_statement219);
            opt_eol();
            following.pop();

            following.push(FOLLOW_import_name_in_import_statement223);
            name=import_name();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:324:51: ( ';' )?
            int alt6=2;
            int LA6_0 = input.LA(1);
            if ( LA6_0==15 ) {
                alt6=1;
            }
            else if ( LA6_0==-1||LA6_0==EOL||LA6_0==17||(LA6_0>=20 && LA6_0<=22)||LA6_0==28||(LA6_0>=30 && LA6_0<=31) ) {
                alt6=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("324:51: ( \';\' )?", 6, 0, input);

                throw nvae;
            }
            switch (alt6) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:324:51: ';'
                    {
                    match(input,15,FOLLOW_15_in_import_statement225); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_import_statement228);
            opt_eol();
            following.pop();

            
            			if (packageDescr != null) 
            				packageDescr.addImport( name );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end import_statement


    // $ANTLR start import_name
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:331:1: import_name returns [String name] : id= ID ( '.' id= ID )* (star= '.*' )? ;
    public String import_name() throws RecognitionException {   
        String name;
        Token id=null;
        Token star=null;

        
        		name = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:17: (id= ID ( '.' id= ID )* (star= '.*' )? )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:17: id= ID ( '.' id= ID )* (star= '.*' )?
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_import_name259); 
             name=id.getText(); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:46: ( '.' id= ID )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);
                if ( LA7_0==18 ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:48: '.' id= ID
            	    {
            	    match(input,18,FOLLOW_18_in_import_name265); 
            	    id=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_import_name269); 
            	     name = name + "." + id.getText(); 

            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:99: (star= '.*' )?
            int alt8=2;
            int LA8_0 = input.LA(1);
            if ( LA8_0==19 ) {
                alt8=1;
            }
            else if ( LA8_0==-1||LA8_0==EOL||LA8_0==15||LA8_0==17||(LA8_0>=20 && LA8_0<=22)||LA8_0==28||(LA8_0>=30 && LA8_0<=31) ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("336:99: (star= \'.*\' )?", 8, 0, input);

                throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:336:100: star= '.*'
                    {
                    star=(Token)input.LT(1);
                    match(input,19,FOLLOW_19_in_import_name279); 
                     name = name + star.getText(); 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return name;
    }
    // $ANTLR end import_name


    // $ANTLR start expander
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:338:1: expander : 'expander' (name= dotted_name )? ( ';' )? opt_eol ;
    public void expander() throws RecognitionException {   
        String name = null;


        
        		String config=null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:17: ( 'expander' (name= dotted_name )? ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:17: 'expander' (name= dotted_name )? ( ';' )? opt_eol
            {
            match(input,20,FOLLOW_20_in_expander299); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:28: (name= dotted_name )?
            int alt9=2;
            int LA9_0 = input.LA(1);
            if ( LA9_0==ID ) {
                alt9=1;
            }
            else if ( LA9_0==-1||LA9_0==EOL||LA9_0==15||LA9_0==17||(LA9_0>=20 && LA9_0<=22)||LA9_0==28||(LA9_0>=30 && LA9_0<=31) ) {
                alt9=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("342:28: (name= dotted_name )?", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:29: name= dotted_name
                    {
                    following.push(FOLLOW_dotted_name_in_expander304);
                    name=dotted_name();
                    following.pop();


                    }
                    break;

            }

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:48: ( ';' )?
            int alt10=2;
            int LA10_0 = input.LA(1);
            if ( LA10_0==15 ) {
                alt10=1;
            }
            else if ( LA10_0==-1||LA10_0==EOL||LA10_0==17||(LA10_0>=20 && LA10_0<=22)||LA10_0==28||(LA10_0>=30 && LA10_0<=31) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("342:48: ( \';\' )?", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:342:48: ';'
                    {
                    match(input,15,FOLLOW_15_in_expander308); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_expander311);
            opt_eol();
            following.pop();

            
            			if (expanderResolver == null) 
            				throw new IllegalArgumentException("Unable to use expander. Make sure a expander or dsl config is being passed to the parser. [ExpanderResolver was not set].");
            			if ( expander != null )
            				throw new IllegalArgumentException( "Only one 'expander' statement per file is allowed" );
            			expander = expanderResolver.get( name, config );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end expander


    // $ANTLR start global
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:352:1: global : 'global' type= dotted_name id= ID ( ';' )? opt_eol ;
    public void global() throws RecognitionException {   
        Token id=null;
        String type = null;


        
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:356:17: ( 'global' type= dotted_name id= ID ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:356:17: 'global' type= dotted_name id= ID ( ';' )? opt_eol
            {
            match(input,21,FOLLOW_21_in_global335); 
            following.push(FOLLOW_dotted_name_in_global339);
            type=dotted_name();
            following.pop();

            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_global343); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:356:49: ( ';' )?
            int alt11=2;
            int LA11_0 = input.LA(1);
            if ( LA11_0==15 ) {
                alt11=1;
            }
            else if ( LA11_0==-1||LA11_0==EOL||LA11_0==17||(LA11_0>=20 && LA11_0<=22)||LA11_0==28||(LA11_0>=30 && LA11_0<=31) ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("356:49: ( \';\' )?", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:356:49: ';'
                    {
                    match(input,15,FOLLOW_15_in_global345); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_global348);
            opt_eol();
            following.pop();

            
            			packageDescr.addGlobal( id.getText(), type );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end global


    // $ANTLR start function
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:362:1: function : 'function' opt_eol (retType= dotted_name )? opt_eol name= ID opt_eol '(' opt_eol ( (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )* )? ')' opt_eol '{' body= curly_chunk '}' opt_eol ;
    public void function() throws RecognitionException {   
        Token name=null;
        String retType = null;

        String paramType = null;

        String paramName = null;

        String body = null;


        
        		FunctionDescr f = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:367:17: ( 'function' opt_eol (retType= dotted_name )? opt_eol name= ID opt_eol '(' opt_eol ( (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )* )? ')' opt_eol '{' body= curly_chunk '}' opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:367:17: 'function' opt_eol (retType= dotted_name )? opt_eol name= ID opt_eol '(' opt_eol ( (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )* )? ')' opt_eol '{' body= curly_chunk '}' opt_eol
            {
            match(input,22,FOLLOW_22_in_function372); 
            following.push(FOLLOW_opt_eol_in_function374);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:367:36: (retType= dotted_name )?
            int alt12=2;
            alt12 = dfa12.predict(input); 
            switch (alt12) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:367:37: retType= dotted_name
                    {
                    following.push(FOLLOW_dotted_name_in_function379);
                    retType=dotted_name();
                    following.pop();


                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_function383);
            opt_eol();
            following.pop();

            name=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_function387); 
            following.push(FOLLOW_opt_eol_in_function389);
            opt_eol();
            following.pop();

            
            			//System.err.println( "function :: " + name.getText() );
            			f = new FunctionDescr( name.getText(), retType );
            		
            match(input,23,FOLLOW_23_in_function398); 
            following.push(FOLLOW_opt_eol_in_function400);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:373:25: ( (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )* )?
            int alt16=2;
            int LA16_0 = input.LA(1);
            if ( (LA16_0>=EOL && LA16_0<=ID)||LA16_0==15 ) {
                alt16=1;
            }
            else if ( LA16_0==25 ) {
                alt16=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("373:25: ( (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( \',\' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )* )?", 16, 0, input);

                throw nvae;
            }
            switch (alt16) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:373:33: (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )*
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:373:33: (paramType= dotted_name )?
                    int alt13=2;
                    alt13 = dfa13.predict(input); 
                    switch (alt13) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:373:34: paramType= dotted_name
                            {
                            following.push(FOLLOW_dotted_name_in_function410);
                            paramType=dotted_name();
                            following.pop();


                            }
                            break;

                    }

                    following.push(FOLLOW_opt_eol_in_function414);
                    opt_eol();
                    following.pop();

                    following.push(FOLLOW_argument_name_in_function418);
                    paramName=argument_name();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_function420);
                    opt_eol();
                    following.pop();

                    
                    					f.addParameter( paramType, paramName );
                    				
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:377:33: ( ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol )*
                    loop15:
                    do {
                        int alt15=2;
                        int LA15_0 = input.LA(1);
                        if ( LA15_0==24 ) {
                            alt15=1;
                        }


                        switch (alt15) {
                    	case 1 :
                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:377:41: ',' opt_eol (paramType= dotted_name )? opt_eol paramName= argument_name opt_eol
                    	    {
                    	    match(input,24,FOLLOW_24_in_function434); 
                    	    following.push(FOLLOW_opt_eol_in_function436);
                    	    opt_eol();
                    	    following.pop();

                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:377:53: (paramType= dotted_name )?
                    	    int alt14=2;
                    	    alt14 = dfa14.predict(input); 
                    	    switch (alt14) {
                    	        case 1 :
                    	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:377:54: paramType= dotted_name
                    	            {
                    	            following.push(FOLLOW_dotted_name_in_function441);
                    	            paramType=dotted_name();
                    	            following.pop();


                    	            }
                    	            break;

                    	    }

                    	    following.push(FOLLOW_opt_eol_in_function445);
                    	    opt_eol();
                    	    following.pop();

                    	    following.push(FOLLOW_argument_name_in_function449);
                    	    paramName=argument_name();
                    	    following.pop();

                    	    following.push(FOLLOW_opt_eol_in_function451);
                    	    opt_eol();
                    	    following.pop();

                    	    
                    	    						f.addParameter( paramType, paramName );
                    	    					

                    	    }
                    	    break;

                    	default :
                    	    break loop15;
                        }
                    } while (true);


                    }
                    break;

            }

            match(input,25,FOLLOW_25_in_function476); 
            following.push(FOLLOW_opt_eol_in_function480);
            opt_eol();
            following.pop();

            match(input,26,FOLLOW_26_in_function484); 
            following.push(FOLLOW_curly_chunk_in_function491);
            body=curly_chunk();
            following.pop();

            
            				f.setText( body );
            			
            match(input,27,FOLLOW_27_in_function500); 
            
            			packageDescr.addFunction( f );
            		
            following.push(FOLLOW_opt_eol_in_function508);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end function


    // $ANTLR start query
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:398:1: query returns [QueryDescr query] : opt_eol loc= 'query' queryName= word opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) 'end' opt_eol ;
    public QueryDescr query() throws RecognitionException {   
        QueryDescr query;
        Token loc=null;
        String queryName = null;


        
        		query = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:403:17: ( opt_eol loc= 'query' queryName= word opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) 'end' opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:403:17: opt_eol loc= 'query' queryName= word opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) 'end' opt_eol
            {
            following.push(FOLLOW_opt_eol_in_query532);
            opt_eol();
            following.pop();

            loc=(Token)input.LT(1);
            match(input,28,FOLLOW_28_in_query538); 
            following.push(FOLLOW_word_in_query542);
            queryName=word();
            following.pop();

            following.push(FOLLOW_opt_eol_in_query544);
            opt_eol();
            following.pop();

             
            			query = new QueryDescr( queryName, null ); 
            			query.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            			AndDescr lhs = new AndDescr(); query.setLhs( lhs ); 
            			lhs.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )
            int alt17=2;
            switch ( input.LA(1) ) {
            case 23:
                int LA17_1 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 1, input);

                    throw nvae;
                }
                break;
            case EOL:
                int LA17_2 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 2, input);

                    throw nvae;
                }
                break;
            case 29:
                int LA17_3 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 3, input);

                    throw nvae;
                }
                break;
            case 52:
                int LA17_4 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 4, input);

                    throw nvae;
                }
                break;
            case 53:
                int LA17_5 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 5, input);

                    throw nvae;
                }
                break;
            case 54:
                int LA17_6 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 6, input);

                    throw nvae;
                }
                break;
            case ID:
                int LA17_7 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 7, input);

                    throw nvae;
                }
                break;
            case 15:
                int LA17_8 = input.LA(2);
                if (  expander != null  ) {
                    alt17=1;
                }
                else if ( true ) {
                    alt17=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 8, input);

                    throw nvae;
                }
                break;
            case INT:
            case BOOL:
            case STRING:
            case FLOAT:
            case MISC:
            case WS:
            case SH_STYLE_SINGLE_LINE_COMMENT:
            case C_STYLE_SINGLE_LINE_COMMENT:
            case MULTI_LINE_COMMENT:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
                alt17=1;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("411:17: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 17, 0, input);

                throw nvae;
            }

            switch (alt17) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:412:25: {...}? expander_lhs_block[lhs]
                    {
                    if ( !( expander != null ) ) {
                        throw new FailedPredicateException(input, "query", " expander != null ");
                    }
                    following.push(FOLLOW_expander_lhs_block_in_query560);
                    expander_lhs_block(lhs);
                    following.pop();


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:413:27: normal_lhs_block[lhs]
                    {
                    following.push(FOLLOW_normal_lhs_block_in_query568);
                    normal_lhs_block(lhs);
                    following.pop();


                    }
                    break;

            }

            match(input,29,FOLLOW_29_in_query583); 
            following.push(FOLLOW_opt_eol_in_query585);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return query;
    }
    // $ANTLR end query


    // $ANTLR start template
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:419:1: template returns [FactTemplateDescr template] : opt_eol loc= 'template' templateName= ID EOL (slot= template_slot )+ 'end' EOL ;
    public FactTemplateDescr template() throws RecognitionException {   
        FactTemplateDescr template;
        Token loc=null;
        Token templateName=null;
        FieldTemplateDescr slot = null;


        
        		template = null;		
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:424:17: ( opt_eol loc= 'template' templateName= ID EOL (slot= template_slot )+ 'end' EOL )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:424:17: opt_eol loc= 'template' templateName= ID EOL (slot= template_slot )+ 'end' EOL
            {
            following.push(FOLLOW_opt_eol_in_template609);
            opt_eol();
            following.pop();

            loc=(Token)input.LT(1);
            match(input,30,FOLLOW_30_in_template615); 
            templateName=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_template619); 
            match(input,EOL,FOLLOW_EOL_in_template621); 
            
            			template = new FactTemplateDescr(templateName.getText());
            			template.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );			
            		
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:430:17: (slot= template_slot )+
            int cnt18=0;
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);
                if ( LA18_0==ID ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:431:25: slot= template_slot
            	    {
            	    following.push(FOLLOW_template_slot_in_template636);
            	    slot=template_slot();
            	    following.pop();

            	    
            	    				template.addFieldTemplate(slot);
            	    			

            	    }
            	    break;

            	default :
            	    if ( cnt18 >= 1 ) break loop18;
                        EarlyExitException eee =
                            new EarlyExitException(18, input);
                        throw eee;
                }
                cnt18++;
            } while (true);

            match(input,29,FOLLOW_29_in_template651); 
            match(input,EOL,FOLLOW_EOL_in_template653); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return template;
    }
    // $ANTLR end template


    // $ANTLR start template_slot
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:439:1: template_slot returns [FieldTemplateDescr field] : fieldType= dotted_name name= ID (EOL|';');
    public FieldTemplateDescr template_slot() throws RecognitionException {   
        FieldTemplateDescr field;
        Token name=null;
        String fieldType = null;


        
        		field = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:445:18: (fieldType= dotted_name name= ID (EOL|';'))
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:445:18: fieldType= dotted_name name= ID (EOL|';')
            {
            following.push(FOLLOW_dotted_name_in_template_slot685);
            fieldType=dotted_name();
            following.pop();

            name=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_template_slot689); 
            if ( input.LA(1)==EOL||input.LA(1)==15 ) {
                input.consume();
                errorRecovery=false;
            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recoverFromMismatchedSet(input,mse,FOLLOW_set_in_template_slot693);    throw mse;
            }

            
            			
            			
            			field = new FieldTemplateDescr(name.getText(), fieldType);
            			field.setLocation( offset(name.getLine()), name.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return field;
    }
    // $ANTLR end template_slot


    // $ANTLR start rule
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:454:1: rule returns [RuleDescr rule] : opt_eol loc= 'rule' ruleName= word opt_eol ( rule_attributes[rule] )? opt_eol ( (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )? )? 'end' opt_eol ;
    public RuleDescr rule() throws RecognitionException {   
        RuleDescr rule;
        Token loc=null;
        Token any=null;
        String ruleName = null;


        
        		rule = null;
        		String consequence = "";
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:460:17: ( opt_eol loc= 'rule' ruleName= word opt_eol ( rule_attributes[rule] )? opt_eol ( (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )? )? 'end' opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:460:17: opt_eol loc= 'rule' ruleName= word opt_eol ( rule_attributes[rule] )? opt_eol ( (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )? )? 'end' opt_eol
            {
            following.push(FOLLOW_opt_eol_in_rule728);
            opt_eol();
            following.pop();

            loc=(Token)input.LT(1);
            match(input,31,FOLLOW_31_in_rule734); 
            following.push(FOLLOW_word_in_rule738);
            ruleName=word();
            following.pop();

            following.push(FOLLOW_opt_eol_in_rule740);
            opt_eol();
            following.pop();

             
            			debug( "start rule: " + ruleName );
            			rule = new RuleDescr( ruleName, null ); 
            			rule.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:467:17: ( rule_attributes[rule] )?
            int alt19=2;
            switch ( input.LA(1) ) {
            case 33:
            case 35:
                alt19=1;
                break;
            case EOL:
            case 15:
            case 24:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
                alt19=1;
                break;
            case 32:
                alt19=1;
                break;
            case 34:
                alt19=1;
                break;
            case 29:
                alt19=1;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("467:17: ( rule_attributes[rule] )?", 19, 0, input);

                throw nvae;
            }

            switch (alt19) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:467:25: rule_attributes[rule]
                    {
                    following.push(FOLLOW_rule_attributes_in_rule751);
                    rule_attributes(rule);
                    following.pop();


                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_rule761);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:17: ( (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )? )?
            int alt26=2;
            int LA26_0 = input.LA(1);
            if ( LA26_0==EOL||LA26_0==15||LA26_0==32||LA26_0==34 ) {
                alt26=1;
            }
            else if ( LA26_0==29 ) {
                alt26=1;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("470:17: ( (loc= \'when\' ( \':\' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= \'then\' ( \':\' )? opt_eol ( options {greedy=false; } : any= . )* )? )?", 26, 0, input);

                throw nvae;
            }
            switch (alt26) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:18: (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )? ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )?
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:18: (loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )?
                    int alt22=2;
                    int LA22_0 = input.LA(1);
                    if ( LA22_0==32 ) {
                        alt22=1;
                    }
                    else if ( LA22_0==EOL||LA22_0==15||LA22_0==29||LA22_0==34 ) {
                        alt22=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("470:18: (loc= \'when\' ( \':\' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] ) )?", 22, 0, input);

                        throw nvae;
                    }
                    switch (alt22) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:25: loc= 'when' ( ':' )? opt_eol ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )
                            {
                            loc=(Token)input.LT(1);
                            match(input,32,FOLLOW_32_in_rule770); 
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:36: ( ':' )?
                            int alt20=2;
                            int LA20_0 = input.LA(1);
                            if ( LA20_0==33 ) {
                                int LA20_1 = input.LA(2);
                                if ( !( expander != null ) ) {
                                    alt20=1;
                                }
                                else if (  expander != null  ) {
                                    alt20=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("470:36: ( \':\' )?", 20, 1, input);

                                    throw nvae;
                                }
                            }
                            else if ( (LA20_0>=EOL && LA20_0<=32)||(LA20_0>=34 && LA20_0<=67) ) {
                                alt20=2;
                            }
                            else {
                                NoViableAltException nvae =
                                    new NoViableAltException("470:36: ( \':\' )?", 20, 0, input);

                                throw nvae;
                            }
                            switch (alt20) {
                                case 1 :
                                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:470:36: ':'
                                    {
                                    match(input,33,FOLLOW_33_in_rule772); 

                                    }
                                    break;

                            }

                            following.push(FOLLOW_opt_eol_in_rule775);
                            opt_eol();
                            following.pop();

                             
                            				AndDescr lhs = new AndDescr(); rule.setLhs( lhs ); 
                            				lhs.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
                            			
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )
                            int alt21=2;
                            switch ( input.LA(1) ) {
                            case 23:
                                int LA21_1 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 1, input);

                                    throw nvae;
                                }
                                break;
                            case EOL:
                                int LA21_2 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 2, input);

                                    throw nvae;
                                }
                                break;
                            case 15:
                                int LA21_3 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 3, input);

                                    throw nvae;
                                }
                                break;
                            case 34:
                                int LA21_4 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 4, input);

                                    throw nvae;
                                }
                                break;
                            case 29:
                                int LA21_5 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 5, input);

                                    throw nvae;
                                }
                                break;
                            case 52:
                                int LA21_6 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 6, input);

                                    throw nvae;
                                }
                                break;
                            case 53:
                                int LA21_7 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 7, input);

                                    throw nvae;
                                }
                                break;
                            case 54:
                                int LA21_8 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 8, input);

                                    throw nvae;
                                }
                                break;
                            case ID:
                                int LA21_9 = input.LA(2);
                                if (  expander != null  ) {
                                    alt21=1;
                                }
                                else if ( true ) {
                                    alt21=2;
                                }
                                else {
                                    NoViableAltException nvae =
                                        new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 9, input);

                                    throw nvae;
                                }
                                break;
                            case INT:
                            case BOOL:
                            case STRING:
                            case FLOAT:
                            case MISC:
                            case WS:
                            case SH_STYLE_SINGLE_LINE_COMMENT:
                            case C_STYLE_SINGLE_LINE_COMMENT:
                            case MULTI_LINE_COMMENT:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 24:
                            case 25:
                            case 26:
                            case 27:
                            case 28:
                            case 30:
                            case 31:
                            case 32:
                            case 33:
                            case 35:
                            case 36:
                            case 37:
                            case 38:
                            case 39:
                            case 40:
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                            case 49:
                            case 50:
                            case 51:
                            case 55:
                            case 56:
                            case 57:
                            case 58:
                            case 59:
                            case 60:
                            case 61:
                            case 62:
                            case 63:
                            case 64:
                            case 65:
                            case 66:
                            case 67:
                                alt21=1;
                                break;
                            default:
                                NoViableAltException nvae =
                                    new NoViableAltException("475:25: ({...}? expander_lhs_block[lhs] | normal_lhs_block[lhs] )", 21, 0, input);

                                throw nvae;
                            }

                            switch (alt21) {
                                case 1 :
                                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:476:33: {...}? expander_lhs_block[lhs]
                                    {
                                    if ( !( expander != null ) ) {
                                        throw new FailedPredicateException(input, "rule", " expander != null ");
                                    }
                                    following.push(FOLLOW_expander_lhs_block_in_rule793);
                                    expander_lhs_block(lhs);
                                    following.pop();


                                    }
                                    break;
                                case 2 :
                                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:477:35: normal_lhs_block[lhs]
                                    {
                                    following.push(FOLLOW_normal_lhs_block_in_rule802);
                                    normal_lhs_block(lhs);
                                    following.pop();


                                    }
                                    break;

                            }


                            }
                            break;

                    }

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:481:17: ( opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )* )?
                    int alt25=2;
                    int LA25_0 = input.LA(1);
                    if ( LA25_0==EOL||LA25_0==15||LA25_0==34 ) {
                        alt25=1;
                    }
                    else if ( LA25_0==29 ) {
                        alt25=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("481:17: ( opt_eol loc= \'then\' ( \':\' )? opt_eol ( options {greedy=false; } : any= . )* )?", 25, 0, input);

                        throw nvae;
                    }
                    switch (alt25) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:481:19: opt_eol loc= 'then' ( ':' )? opt_eol ( options {greedy=false; } : any= . )*
                            {
                            following.push(FOLLOW_opt_eol_in_rule825);
                            opt_eol();
                            following.pop();

                            loc=(Token)input.LT(1);
                            match(input,34,FOLLOW_34_in_rule829); 
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:481:38: ( ':' )?
                            int alt23=2;
                            int LA23_0 = input.LA(1);
                            if ( LA23_0==33 ) {
                                alt23=1;
                            }
                            else if ( (LA23_0>=EOL && LA23_0<=32)||(LA23_0>=34 && LA23_0<=67) ) {
                                alt23=2;
                            }
                            else {
                                NoViableAltException nvae =
                                    new NoViableAltException("481:38: ( \':\' )?", 23, 0, input);

                                throw nvae;
                            }
                            switch (alt23) {
                                case 1 :
                                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:481:38: ':'
                                    {
                                    match(input,33,FOLLOW_33_in_rule831); 

                                    }
                                    break;

                            }

                            following.push(FOLLOW_opt_eol_in_rule835);
                            opt_eol();
                            following.pop();

                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:482:25: ( options {greedy=false; } : any= . )*
                            loop24:
                            do {
                                int alt24=2;
                                int LA24_0 = input.LA(1);
                                if ( LA24_0==29 ) {
                                    alt24=2;
                                }
                                else if ( (LA24_0>=EOL && LA24_0<=28)||(LA24_0>=30 && LA24_0<=67) ) {
                                    alt24=1;
                                }


                                switch (alt24) {
                            	case 1 :
                            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:482:52: any= .
                            	    {
                            	    any=(Token)input.LT(1);
                            	    matchAny(input); 
                            	    
                            	    					consequence = consequence + " " + any.getText();
                            	    				

                            	    }
                            	    break;

                            	default :
                            	    break loop24;
                                }
                            } while (true);

                            
                            				if ( expander != null ) {
                            					String expanded = runThenExpander( consequence, offset(loc.getLine()) );
                            					rule.setConsequence( expanded );
                            				} else { 
                            					rule.setConsequence( consequence ); 
                            				}
                            				rule.setConsequenceLocation(offset(loc.getLine()), loc.getCharPositionInLine());
                            			

                            }
                            break;

                    }


                    }
                    break;

            }

            match(input,29,FOLLOW_29_in_rule881); 
            following.push(FOLLOW_opt_eol_in_rule883);
            opt_eol();
            following.pop();

            
            			debug( "end rule: " + ruleName );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return rule;
    }
    // $ANTLR end rule


    // $ANTLR start extra_statement
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:503:1: extra_statement : ( import_statement | global | function ) ;
    public void extra_statement() throws RecognitionException {   
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:505:9: ( ( import_statement | global | function ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:505:9: ( import_statement | global | function )
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:505:9: ( import_statement | global | function )
            int alt27=3;
            switch ( input.LA(1) ) {
            case 17:
                alt27=1;
                break;
            case 21:
                alt27=2;
                break;
            case 22:
                alt27=3;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("505:9: ( import_statement | global | function )", 27, 0, input);

                throw nvae;
            }

            switch (alt27) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:505:17: import_statement
                    {
                    following.push(FOLLOW_import_statement_in_extra_statement903);
                    import_statement();
                    following.pop();


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:506:17: global
                    {
                    following.push(FOLLOW_global_in_extra_statement908);
                    global();
                    following.pop();


                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:507:17: function
                    {
                    following.push(FOLLOW_function_in_extra_statement913);
                    function();
                    following.pop();


                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end extra_statement


    // $ANTLR start rule_attributes
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:511:1: rule_attributes[RuleDescr rule] : ( 'attributes' )? ( ':' )? opt_eol ( ( ',' )? a= rule_attribute opt_eol )* ;
    public void rule_attributes(RuleDescr rule) throws RecognitionException {   
        AttributeDescr a = null;


        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:25: ( ( 'attributes' )? ( ':' )? opt_eol ( ( ',' )? a= rule_attribute opt_eol )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:25: ( 'attributes' )? ( ':' )? opt_eol ( ( ',' )? a= rule_attribute opt_eol )*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:25: ( 'attributes' )?
            int alt28=2;
            int LA28_0 = input.LA(1);
            if ( LA28_0==35 ) {
                alt28=1;
            }
            else if ( LA28_0==EOL||LA28_0==15||LA28_0==24||LA28_0==29||(LA28_0>=32 && LA28_0<=34)||(LA28_0>=36 && LA28_0<=41) ) {
                alt28=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("513:25: ( \'attributes\' )?", 28, 0, input);

                throw nvae;
            }
            switch (alt28) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:25: 'attributes'
                    {
                    match(input,35,FOLLOW_35_in_rule_attributes932); 

                    }
                    break;

            }

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:39: ( ':' )?
            int alt29=2;
            int LA29_0 = input.LA(1);
            if ( LA29_0==33 ) {
                alt29=1;
            }
            else if ( LA29_0==EOL||LA29_0==15||LA29_0==24||LA29_0==29||LA29_0==32||LA29_0==34||(LA29_0>=36 && LA29_0<=41) ) {
                alt29=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("513:39: ( \':\' )?", 29, 0, input);

                throw nvae;
            }
            switch (alt29) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:513:39: ':'
                    {
                    match(input,33,FOLLOW_33_in_rule_attributes935); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_rule_attributes938);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:514:25: ( ( ',' )? a= rule_attribute opt_eol )*
            loop31:
            do {
                int alt31=2;
                int LA31_0 = input.LA(1);
                if ( LA31_0==24||(LA31_0>=36 && LA31_0<=41) ) {
                    alt31=1;
                }


                switch (alt31) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:514:33: ( ',' )? a= rule_attribute opt_eol
            	    {
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:514:33: ( ',' )?
            	    int alt30=2;
            	    int LA30_0 = input.LA(1);
            	    if ( LA30_0==24 ) {
            	        alt30=1;
            	    }
            	    else if ( (LA30_0>=36 && LA30_0<=41) ) {
            	        alt30=2;
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("514:33: ( \',\' )?", 30, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt30) {
            	        case 1 :
            	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:514:33: ','
            	            {
            	            match(input,24,FOLLOW_24_in_rule_attributes945); 

            	            }
            	            break;

            	    }

            	    following.push(FOLLOW_rule_attribute_in_rule_attributes950);
            	    a=rule_attribute();
            	    following.pop();

            	    following.push(FOLLOW_opt_eol_in_rule_attributes952);
            	    opt_eol();
            	    following.pop();

            	    
            	    					rule.addAttribute( a );
            	    				

            	    }
            	    break;

            	default :
            	    break loop31;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end rule_attributes


    // $ANTLR start rule_attribute
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:521:1: rule_attribute returns [AttributeDescr d] : (a= salience | a= no_loop | a= agenda_group | a= duration | a= activation_group | a= auto_focus );
    public AttributeDescr rule_attribute() throws RecognitionException {   
        AttributeDescr d;
        AttributeDescr a = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:526:25: (a= salience | a= no_loop | a= agenda_group | a= duration | a= activation_group | a= auto_focus )
            int alt32=6;
            switch ( input.LA(1) ) {
            case 36:
                alt32=1;
                break;
            case 37:
                alt32=2;
                break;
            case 40:
                alt32=3;
                break;
            case 41:
                alt32=4;
                break;
            case 39:
                alt32=5;
                break;
            case 38:
                alt32=6;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("521:1: rule_attribute returns [AttributeDescr d] : (a= salience | a= no_loop | a= agenda_group | a= duration | a= activation_group | a= auto_focus );", 32, 0, input);

                throw nvae;
            }

            switch (alt32) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:526:25: a= salience
                    {
                    following.push(FOLLOW_salience_in_rule_attribute991);
                    a=salience();
                    following.pop();

                     d = a; 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:527:25: a= no_loop
                    {
                    following.push(FOLLOW_no_loop_in_rule_attribute1001);
                    a=no_loop();
                    following.pop();

                     d = a; 

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:528:25: a= agenda_group
                    {
                    following.push(FOLLOW_agenda_group_in_rule_attribute1012);
                    a=agenda_group();
                    following.pop();

                     d = a; 

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:529:25: a= duration
                    {
                    following.push(FOLLOW_duration_in_rule_attribute1025);
                    a=duration();
                    following.pop();

                     d = a; 

                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:530:25: a= activation_group
                    {
                    following.push(FOLLOW_activation_group_in_rule_attribute1039);
                    a=activation_group();
                    following.pop();

                     d = a; 

                    }
                    break;
                case 6 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:531:25: a= auto_focus
                    {
                    following.push(FOLLOW_auto_focus_in_rule_attribute1050);
                    a=auto_focus();
                    following.pop();

                     d = a; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end rule_attribute


    // $ANTLR start salience
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:535:1: salience returns [AttributeDescr d ] : loc= 'salience' opt_eol i= INT ( ';' )? opt_eol ;
    public AttributeDescr salience() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token i=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:540:17: (loc= 'salience' opt_eol i= INT ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:540:17: loc= 'salience' opt_eol i= INT ( ';' )? opt_eol
            {
            loc=(Token)input.LT(1);
            match(input,36,FOLLOW_36_in_salience1083); 
            following.push(FOLLOW_opt_eol_in_salience1085);
            opt_eol();
            following.pop();

            i=(Token)input.LT(1);
            match(input,INT,FOLLOW_INT_in_salience1089); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:540:46: ( ';' )?
            int alt33=2;
            int LA33_0 = input.LA(1);
            if ( LA33_0==15 ) {
                alt33=1;
            }
            else if ( LA33_0==EOL||LA33_0==24||LA33_0==29||LA33_0==32||LA33_0==34||(LA33_0>=36 && LA33_0<=41) ) {
                alt33=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("540:46: ( \';\' )?", 33, 0, input);

                throw nvae;
            }
            switch (alt33) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:540:46: ';'
                    {
                    match(input,15,FOLLOW_15_in_salience1091); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_salience1094);
            opt_eol();
            following.pop();

            
            			d = new AttributeDescr( "salience", i.getText() );
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end salience


    // $ANTLR start no_loop
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:547:1: no_loop returns [AttributeDescr d] : ( (loc= 'no-loop' opt_eol ( ';' )? opt_eol ) | (loc= 'no-loop' t= BOOL opt_eol ( ';' )? opt_eol ) );
    public AttributeDescr no_loop() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token t=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:552:17: ( (loc= 'no-loop' opt_eol ( ';' )? opt_eol ) | (loc= 'no-loop' t= BOOL opt_eol ( ';' )? opt_eol ) )
            int alt36=2;
            int LA36_0 = input.LA(1);
            if ( LA36_0==37 ) {
                int LA36_1 = input.LA(2);
                if ( LA36_1==BOOL ) {
                    alt36=2;
                }
                else if ( LA36_1==EOL||LA36_1==15||LA36_1==24||LA36_1==29||LA36_1==32||LA36_1==34||(LA36_1>=36 && LA36_1<=41) ) {
                    alt36=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("547:1: no_loop returns [AttributeDescr d] : ( (loc= \'no-loop\' opt_eol ( \';\' )? opt_eol ) | (loc= \'no-loop\' t= BOOL opt_eol ( \';\' )? opt_eol ) );", 36, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("547:1: no_loop returns [AttributeDescr d] : ( (loc= \'no-loop\' opt_eol ( \';\' )? opt_eol ) | (loc= \'no-loop\' t= BOOL opt_eol ( \';\' )? opt_eol ) );", 36, 0, input);

                throw nvae;
            }
            switch (alt36) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:552:17: (loc= 'no-loop' opt_eol ( ';' )? opt_eol )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:552:17: (loc= 'no-loop' opt_eol ( ';' )? opt_eol )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:553:25: loc= 'no-loop' opt_eol ( ';' )? opt_eol
                    {
                    loc=(Token)input.LT(1);
                    match(input,37,FOLLOW_37_in_no_loop1129); 
                    following.push(FOLLOW_opt_eol_in_no_loop1131);
                    opt_eol();
                    following.pop();

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:553:47: ( ';' )?
                    int alt34=2;
                    int LA34_0 = input.LA(1);
                    if ( LA34_0==15 ) {
                        alt34=1;
                    }
                    else if ( LA34_0==EOL||LA34_0==24||LA34_0==29||LA34_0==32||LA34_0==34||(LA34_0>=36 && LA34_0<=41) ) {
                        alt34=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("553:47: ( \';\' )?", 34, 0, input);

                        throw nvae;
                    }
                    switch (alt34) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:553:47: ';'
                            {
                            match(input,15,FOLLOW_15_in_no_loop1133); 

                            }
                            break;

                    }

                    following.push(FOLLOW_opt_eol_in_no_loop1136);
                    opt_eol();
                    following.pop();

                    
                    				d = new AttributeDescr( "no-loop", "true" );
                    				d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
                    			

                    }


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:560:17: (loc= 'no-loop' t= BOOL opt_eol ( ';' )? opt_eol )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:560:17: (loc= 'no-loop' t= BOOL opt_eol ( ';' )? opt_eol )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:561:25: loc= 'no-loop' t= BOOL opt_eol ( ';' )? opt_eol
                    {
                    loc=(Token)input.LT(1);
                    match(input,37,FOLLOW_37_in_no_loop1161); 
                    t=(Token)input.LT(1);
                    match(input,BOOL,FOLLOW_BOOL_in_no_loop1165); 
                    following.push(FOLLOW_opt_eol_in_no_loop1167);
                    opt_eol();
                    following.pop();

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:561:54: ( ';' )?
                    int alt35=2;
                    int LA35_0 = input.LA(1);
                    if ( LA35_0==15 ) {
                        alt35=1;
                    }
                    else if ( LA35_0==EOL||LA35_0==24||LA35_0==29||LA35_0==32||LA35_0==34||(LA35_0>=36 && LA35_0<=41) ) {
                        alt35=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("561:54: ( \';\' )?", 35, 0, input);

                        throw nvae;
                    }
                    switch (alt35) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:561:54: ';'
                            {
                            match(input,15,FOLLOW_15_in_no_loop1169); 

                            }
                            break;

                    }

                    following.push(FOLLOW_opt_eol_in_no_loop1172);
                    opt_eol();
                    following.pop();

                    
                    				d = new AttributeDescr( "no-loop", t.getText() );
                    				d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
                    			

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end no_loop


    // $ANTLR start auto_focus
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:571:1: auto_focus returns [AttributeDescr d] : ( (loc= 'auto-focus' opt_eol ( ';' )? opt_eol ) | (loc= 'auto-focus' t= BOOL opt_eol ( ';' )? opt_eol ) );
    public AttributeDescr auto_focus() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token t=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:576:17: ( (loc= 'auto-focus' opt_eol ( ';' )? opt_eol ) | (loc= 'auto-focus' t= BOOL opt_eol ( ';' )? opt_eol ) )
            int alt39=2;
            int LA39_0 = input.LA(1);
            if ( LA39_0==38 ) {
                int LA39_1 = input.LA(2);
                if ( LA39_1==BOOL ) {
                    alt39=2;
                }
                else if ( LA39_1==EOL||LA39_1==15||LA39_1==24||LA39_1==29||LA39_1==32||LA39_1==34||(LA39_1>=36 && LA39_1<=41) ) {
                    alt39=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("571:1: auto_focus returns [AttributeDescr d] : ( (loc= \'auto-focus\' opt_eol ( \';\' )? opt_eol ) | (loc= \'auto-focus\' t= BOOL opt_eol ( \';\' )? opt_eol ) );", 39, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("571:1: auto_focus returns [AttributeDescr d] : ( (loc= \'auto-focus\' opt_eol ( \';\' )? opt_eol ) | (loc= \'auto-focus\' t= BOOL opt_eol ( \';\' )? opt_eol ) );", 39, 0, input);

                throw nvae;
            }
            switch (alt39) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:576:17: (loc= 'auto-focus' opt_eol ( ';' )? opt_eol )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:576:17: (loc= 'auto-focus' opt_eol ( ';' )? opt_eol )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:577:25: loc= 'auto-focus' opt_eol ( ';' )? opt_eol
                    {
                    loc=(Token)input.LT(1);
                    match(input,38,FOLLOW_38_in_auto_focus1218); 
                    following.push(FOLLOW_opt_eol_in_auto_focus1220);
                    opt_eol();
                    following.pop();

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:577:50: ( ';' )?
                    int alt37=2;
                    int LA37_0 = input.LA(1);
                    if ( LA37_0==15 ) {
                        alt37=1;
                    }
                    else if ( LA37_0==EOL||LA37_0==24||LA37_0==29||LA37_0==32||LA37_0==34||(LA37_0>=36 && LA37_0<=41) ) {
                        alt37=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("577:50: ( \';\' )?", 37, 0, input);

                        throw nvae;
                    }
                    switch (alt37) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:577:50: ';'
                            {
                            match(input,15,FOLLOW_15_in_auto_focus1222); 

                            }
                            break;

                    }

                    following.push(FOLLOW_opt_eol_in_auto_focus1225);
                    opt_eol();
                    following.pop();

                    
                    				d = new AttributeDescr( "auto-focus", "true" );
                    				d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
                    			

                    }


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:584:17: (loc= 'auto-focus' t= BOOL opt_eol ( ';' )? opt_eol )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:584:17: (loc= 'auto-focus' t= BOOL opt_eol ( ';' )? opt_eol )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:585:25: loc= 'auto-focus' t= BOOL opt_eol ( ';' )? opt_eol
                    {
                    loc=(Token)input.LT(1);
                    match(input,38,FOLLOW_38_in_auto_focus1250); 
                    t=(Token)input.LT(1);
                    match(input,BOOL,FOLLOW_BOOL_in_auto_focus1254); 
                    following.push(FOLLOW_opt_eol_in_auto_focus1256);
                    opt_eol();
                    following.pop();

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:585:57: ( ';' )?
                    int alt38=2;
                    int LA38_0 = input.LA(1);
                    if ( LA38_0==15 ) {
                        alt38=1;
                    }
                    else if ( LA38_0==EOL||LA38_0==24||LA38_0==29||LA38_0==32||LA38_0==34||(LA38_0>=36 && LA38_0<=41) ) {
                        alt38=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("585:57: ( \';\' )?", 38, 0, input);

                        throw nvae;
                    }
                    switch (alt38) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:585:57: ';'
                            {
                            match(input,15,FOLLOW_15_in_auto_focus1258); 

                            }
                            break;

                    }

                    following.push(FOLLOW_opt_eol_in_auto_focus1261);
                    opt_eol();
                    following.pop();

                    
                    				d = new AttributeDescr( "auto-focus", t.getText() );
                    				d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
                    			

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end auto_focus


    // $ANTLR start activation_group
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:595:1: activation_group returns [AttributeDescr d] : loc= 'activation-group' opt_eol name= STRING ( ';' )? opt_eol ;
    public AttributeDescr activation_group() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token name=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:600:17: (loc= 'activation-group' opt_eol name= STRING ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:600:17: loc= 'activation-group' opt_eol name= STRING ( ';' )? opt_eol
            {
            loc=(Token)input.LT(1);
            match(input,39,FOLLOW_39_in_activation_group1303); 
            following.push(FOLLOW_opt_eol_in_activation_group1305);
            opt_eol();
            following.pop();

            name=(Token)input.LT(1);
            match(input,STRING,FOLLOW_STRING_in_activation_group1309); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:600:60: ( ';' )?
            int alt40=2;
            int LA40_0 = input.LA(1);
            if ( LA40_0==15 ) {
                alt40=1;
            }
            else if ( LA40_0==EOL||LA40_0==24||LA40_0==29||LA40_0==32||LA40_0==34||(LA40_0>=36 && LA40_0<=41) ) {
                alt40=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("600:60: ( \';\' )?", 40, 0, input);

                throw nvae;
            }
            switch (alt40) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:600:60: ';'
                    {
                    match(input,15,FOLLOW_15_in_activation_group1311); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_activation_group1314);
            opt_eol();
            following.pop();

            
            			d = new AttributeDescr( "activation-group", getString( name ) );
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end activation_group


    // $ANTLR start agenda_group
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:607:1: agenda_group returns [AttributeDescr d] : loc= 'agenda-group' opt_eol name= STRING ( ';' )? opt_eol ;
    public AttributeDescr agenda_group() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token name=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:612:17: (loc= 'agenda-group' opt_eol name= STRING ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:612:17: loc= 'agenda-group' opt_eol name= STRING ( ';' )? opt_eol
            {
            loc=(Token)input.LT(1);
            match(input,40,FOLLOW_40_in_agenda_group1343); 
            following.push(FOLLOW_opt_eol_in_agenda_group1345);
            opt_eol();
            following.pop();

            name=(Token)input.LT(1);
            match(input,STRING,FOLLOW_STRING_in_agenda_group1349); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:612:56: ( ';' )?
            int alt41=2;
            int LA41_0 = input.LA(1);
            if ( LA41_0==15 ) {
                alt41=1;
            }
            else if ( LA41_0==EOL||LA41_0==24||LA41_0==29||LA41_0==32||LA41_0==34||(LA41_0>=36 && LA41_0<=41) ) {
                alt41=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("612:56: ( \';\' )?", 41, 0, input);

                throw nvae;
            }
            switch (alt41) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:612:56: ';'
                    {
                    match(input,15,FOLLOW_15_in_agenda_group1351); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_agenda_group1354);
            opt_eol();
            following.pop();

            
            			d = new AttributeDescr( "agenda-group", getString( name ) );
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end agenda_group


    // $ANTLR start duration
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:620:1: duration returns [AttributeDescr d] : loc= 'duration' opt_eol i= INT ( ';' )? opt_eol ;
    public AttributeDescr duration() throws RecognitionException {   
        AttributeDescr d;
        Token loc=null;
        Token i=null;

        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:625:17: (loc= 'duration' opt_eol i= INT ( ';' )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:625:17: loc= 'duration' opt_eol i= INT ( ';' )? opt_eol
            {
            loc=(Token)input.LT(1);
            match(input,41,FOLLOW_41_in_duration1386); 
            following.push(FOLLOW_opt_eol_in_duration1388);
            opt_eol();
            following.pop();

            i=(Token)input.LT(1);
            match(input,INT,FOLLOW_INT_in_duration1392); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:625:46: ( ';' )?
            int alt42=2;
            int LA42_0 = input.LA(1);
            if ( LA42_0==15 ) {
                alt42=1;
            }
            else if ( LA42_0==EOL||LA42_0==24||LA42_0==29||LA42_0==32||LA42_0==34||(LA42_0>=36 && LA42_0<=41) ) {
                alt42=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("625:46: ( \';\' )?", 42, 0, input);

                throw nvae;
            }
            switch (alt42) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:625:46: ';'
                    {
                    match(input,15,FOLLOW_15_in_duration1394); 

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_duration1397);
            opt_eol();
            following.pop();

            
            			d = new AttributeDescr( "duration", i.getText() );
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end duration


    // $ANTLR start normal_lhs_block
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:633:1: normal_lhs_block[AndDescr descrs] : (d= lhs opt_eol )* opt_eol ;
    public void normal_lhs_block(AndDescr descrs) throws RecognitionException {   
        PatternDescr d = null;


        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:635:17: ( (d= lhs opt_eol )* opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:635:17: (d= lhs opt_eol )* opt_eol
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:635:17: (d= lhs opt_eol )*
            loop43:
            do {
                int alt43=2;
                int LA43_0 = input.LA(1);
                if ( LA43_0==ID||LA43_0==23||(LA43_0>=52 && LA43_0<=54) ) {
                    alt43=1;
                }


                switch (alt43) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:635:25: d= lhs opt_eol
            	    {
            	    following.push(FOLLOW_lhs_in_normal_lhs_block1423);
            	    d=lhs();
            	    following.pop();

            	    following.push(FOLLOW_opt_eol_in_normal_lhs_block1425);
            	    opt_eol();
            	    following.pop();

            	     descrs.addDescr( d ); 

            	    }
            	    break;

            	default :
            	    break loop43;
                }
            } while (true);

            following.push(FOLLOW_opt_eol_in_normal_lhs_block1437);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end normal_lhs_block


    // $ANTLR start expander_lhs_block
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:643:1: expander_lhs_block[AndDescr descrs] : ( options {greedy=false; } : text= paren_chunk loc= EOL ( EOL )* )* ;
    public void expander_lhs_block(AndDescr descrs) throws RecognitionException {   
        Token loc=null;
        String text = null;


        
        		String lhsBlock = null;
        		String eol = System.getProperty( "line.separator" );
        		List constraints = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:650:17: ( ( options {greedy=false; } : text= paren_chunk loc= EOL ( EOL )* )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:650:17: ( options {greedy=false; } : text= paren_chunk loc= EOL ( EOL )* )*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:650:17: ( options {greedy=false; } : text= paren_chunk loc= EOL ( EOL )* )*
            loop45:
            do {
                int alt45=2;
                switch ( input.LA(1) ) {
                case 29:
                    alt45=2;
                    break;
                case EOL:
                    alt45=2;
                    break;
                case 34:
                    alt45=2;
                    break;
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 30:
                case 31:
                case 32:
                case 33:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    alt45=1;
                    break;
                case 15:
                    alt45=2;
                    break;

                }

                switch (alt45) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:651:25: text= paren_chunk loc= EOL ( EOL )*
            	    {
            	    following.push(FOLLOW_paren_chunk_in_expander_lhs_block1478);
            	    text=paren_chunk();
            	    following.pop();

            	    loc=(Token)input.LT(1);
            	    match(input,EOL,FOLLOW_EOL_in_expander_lhs_block1482); 
            	    
            	    				//only expand non null
            	    				if (text != null) {
            	    					if (text.trim().startsWith("-")) {
            	    						if (constraints == null) {
            	    							constraints = new ArrayList();
            	    						}
            	    						constraints.add(runWhenExpander( text, offset(loc.getLine())));
            	    					} else {
            	    						if (constraints != null) {
            	    							lhsBlock = applyConstraints(constraints, lhsBlock);
            	    							constraints = null;
            	    						}
            	    					
            	    					
            	    						if (lhsBlock == null) {					
            	    							lhsBlock = runWhenExpander( text, offset(loc.getLine()));
            	    						} else {
            	    							lhsBlock = lhsBlock + eol + runWhenExpander( text, offset(loc.getLine()));
            	    						}
            	    					}
            	    					text = null;
            	    				}
            	    			
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:677:17: ( EOL )*
            	    loop44:
            	    do {
            	        int alt44=2;
            	        int LA44_0 = input.LA(1);
            	        if ( LA44_0==EOL ) {
            	            alt44=1;
            	        }


            	        switch (alt44) {
            	    	case 1 :
            	    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:677:18: EOL
            	    	    {
            	    	    match(input,EOL,FOLLOW_EOL_in_expander_lhs_block1497); 

            	    	    }
            	    	    break;

            	    	default :
            	    	    break loop44;
            	        }
            	    } while (true);


            	    }
            	    break;

            	default :
            	    break loop45;
                }
            } while (true);

            	
            			//flush out any constraints left handing before the RHS
            			lhsBlock = applyConstraints(constraints, lhsBlock);
            			if (lhsBlock != null) {
            				reparseLhs(lhsBlock, descrs);
            			}
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end expander_lhs_block


    // $ANTLR start lhs
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:691:1: lhs returns [PatternDescr d] : l= lhs_or ;
    public PatternDescr lhs() throws RecognitionException {   
        PatternDescr d;
        PatternDescr l = null;


        
        		d=null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:695:17: (l= lhs_or )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:695:17: l= lhs_or
            {
            following.push(FOLLOW_lhs_or_in_lhs1539);
            l=lhs_or();
            following.pop();

             d = l; 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs


    // $ANTLR start lhs_column
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:699:1: lhs_column returns [PatternDescr d] : (f= fact_binding | f= fact );
    public PatternDescr lhs_column() throws RecognitionException {   
        PatternDescr d;
        PatternDescr f = null;


        
        		d=null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:703:17: (f= fact_binding | f= fact )
            int alt46=2;
            alt46 = dfa46.predict(input); 
            switch (alt46) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:703:17: f= fact_binding
                    {
                    following.push(FOLLOW_fact_binding_in_lhs_column1567);
                    f=fact_binding();
                    following.pop();

                     d = f; 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:704:17: f= fact
                    {
                    following.push(FOLLOW_fact_in_lhs_column1576);
                    f=fact();
                    following.pop();

                     d = f; 

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_column


    // $ANTLR start from_statement
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:707:1: from_statement returns [FromDescr d] : 'from' opt_eol ds= from_source ;
    public FromDescr from_statement() throws RecognitionException {   
        FromDescr d;
        DeclarativeInvokerDescr ds = null;


        
        		d=factory.createFrom();
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:712:17: ( 'from' opt_eol ds= from_source )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:712:17: 'from' opt_eol ds= from_source
            {
            match(input,42,FOLLOW_42_in_from_statement1604); 
            following.push(FOLLOW_opt_eol_in_from_statement1606);
            opt_eol();
            following.pop();

            following.push(FOLLOW_from_source_in_from_statement1610);
            ds=from_source();
            following.pop();

            
             			d.setDataSource(ds);
             		
             		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end from_statement


    // $ANTLR start from_source
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:722:1: from_source returns [DeclarativeInvokerDescr ds] : ( (var= ID '.' field= ID ) | (var= ID '.' method= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' ) | (functionName= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' ) );
    public DeclarativeInvokerDescr from_source() throws RecognitionException {   
        DeclarativeInvokerDescr ds;
        Token var=null;
        Token field=null;
        Token method=null;
        Token functionName=null;
        ArrayList args = null;


        
        		ds = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:727:17: ( (var= ID '.' field= ID ) | (var= ID '.' method= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' ) | (functionName= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' ) )
            int alt47=3;
            alt47 = dfa47.predict(input); 
            switch (alt47) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:727:17: (var= ID '.' field= ID )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:727:17: (var= ID '.' field= ID )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:727:18: var= ID '.' field= ID
                    {
                    var=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_from_source1654); 
                    match(input,18,FOLLOW_18_in_from_source1656); 
                    field=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_from_source1660); 
                    
                    			  FieldAccessDescr fa = new FieldAccessDescr(var.getText(), field.getText());	
                    			  fa.setLine(var.getLine());
                    			  ds = fa;
                    			 

                    }


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:737:17: (var= ID '.' method= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:737:17: (var= ID '.' method= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:737:18: var= ID '.' method= ID opt_eol '(' opt_eol args= argument_list opt_eol ')'
                    {
                    var=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_from_source1687); 
                    match(input,18,FOLLOW_18_in_from_source1689); 
                    method=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_from_source1693); 
                    following.push(FOLLOW_opt_eol_in_from_source1695);
                    opt_eol();
                    following.pop();

                    match(input,23,FOLLOW_23_in_from_source1698); 
                    following.push(FOLLOW_opt_eol_in_from_source1700);
                    opt_eol();
                    following.pop();

                    following.push(FOLLOW_argument_list_in_from_source1704);
                    args=argument_list();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_from_source1706);
                    opt_eol();
                    following.pop();

                    match(input,25,FOLLOW_25_in_from_source1708); 
                    
                    			MethodAccessDescr mc = new MethodAccessDescr(var.getText(), method.getText());
                    			mc.setArguments(args);
                    			mc.setLine(var.getLine());
                    			ds = mc;
                    			

                    }


                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:746:17: (functionName= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' )
                    {
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:746:17: (functionName= ID opt_eol '(' opt_eol args= argument_list opt_eol ')' )
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:746:18: functionName= ID opt_eol '(' opt_eol args= argument_list opt_eol ')'
                    {
                    functionName=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_from_source1730); 
                    following.push(FOLLOW_opt_eol_in_from_source1732);
                    opt_eol();
                    following.pop();

                    match(input,23,FOLLOW_23_in_from_source1734); 
                    following.push(FOLLOW_opt_eol_in_from_source1736);
                    opt_eol();
                    following.pop();

                    following.push(FOLLOW_argument_list_in_from_source1740);
                    args=argument_list();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_from_source1742);
                    opt_eol();
                    following.pop();

                    match(input,25,FOLLOW_25_in_from_source1744); 
                    
                    			FunctionCallDescr fc = new FunctionCallDescr(functionName.getText());
                    			fc.setLine(functionName.getLine());
                    			fc.setArguments(args);
                    			ds = fc;
                    			

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ds;
    }
    // $ANTLR end from_source


    // $ANTLR start argument_list
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:759:1: argument_list returns [ArrayList args] : (param= argument_value ( opt_eol ',' opt_eol param= argument_value )* )? ;
    public ArrayList argument_list() throws RecognitionException {   
        ArrayList args;
        ArgumentValueDescr param = null;


        
        		args = new ArrayList();
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:764:17: ( (param= argument_value ( opt_eol ',' opt_eol param= argument_value )* )? )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:764:17: (param= argument_value ( opt_eol ',' opt_eol param= argument_value )* )?
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:764:17: (param= argument_value ( opt_eol ',' opt_eol param= argument_value )* )?
            int alt49=2;
            int LA49_0 = input.LA(1);
            if ( (LA49_0>=ID && LA49_0<=FLOAT)||LA49_0==26||LA49_0==43 ) {
                alt49=1;
            }
            else if ( LA49_0==EOL||LA49_0==15||LA49_0==25 ) {
                alt49=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("764:17: (param= argument_value ( opt_eol \',\' opt_eol param= argument_value )* )?", 49, 0, input);

                throw nvae;
            }
            switch (alt49) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:764:18: param= argument_value ( opt_eol ',' opt_eol param= argument_value )*
                    {
                    following.push(FOLLOW_argument_value_in_argument_list1787);
                    param=argument_value();
                    following.pop();

                    
                    			if (param != null) {
                    				args.add(param);
                    			}
                    		
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:770:17: ( opt_eol ',' opt_eol param= argument_value )*
                    loop48:
                    do {
                        int alt48=2;
                        alt48 = dfa48.predict(input); 
                        switch (alt48) {
                    	case 1 :
                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:771:25: opt_eol ',' opt_eol param= argument_value
                    	    {
                    	    following.push(FOLLOW_opt_eol_in_argument_list1803);
                    	    opt_eol();
                    	    following.pop();

                    	    match(input,24,FOLLOW_24_in_argument_list1805); 
                    	    following.push(FOLLOW_opt_eol_in_argument_list1807);
                    	    opt_eol();
                    	    following.pop();

                    	    following.push(FOLLOW_argument_value_in_argument_list1811);
                    	    param=argument_value();
                    	    following.pop();

                    	    
                    	    				if (param != null) {
                    	    					args.add(param);
                    	    				}
                    	    			

                    	    }
                    	    break;

                    	default :
                    	    break loop48;
                        }
                    } while (true);


                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return args;
    }
    // $ANTLR end argument_list


    // $ANTLR start argument_value
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:780:1: argument_value returns [ArgumentValueDescr value] : (t= STRING | t= INT | t= FLOAT | t= BOOL | t= ID | t= 'null' | t= 'null' | m= inline_map ) ;
    public ArgumentValueDescr argument_value() throws RecognitionException {   
        ArgumentValueDescr value;
        Token t=null;
        ArgumentValueDescr.MapDescr m = null;


        
        		value = null;
        		String text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:785:17: ( (t= STRING | t= INT | t= FLOAT | t= BOOL | t= ID | t= 'null' | t= 'null' | m= inline_map ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:785:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= ID | t= 'null' | t= 'null' | m= inline_map )
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:785:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= ID | t= 'null' | t= 'null' | m= inline_map )
            int alt50=8;
            switch ( input.LA(1) ) {
            case STRING:
                alt50=1;
                break;
            case INT:
                alt50=2;
                break;
            case FLOAT:
                alt50=3;
                break;
            case BOOL:
                alt50=4;
                break;
            case ID:
                alt50=5;
                break;
            case 43:
                alt50=6;
                break;
            case 26:
                alt50=8;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("785:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= ID | t= \'null\' | t= \'null\' | m= inline_map )", 50, 0, input);

                throw nvae;
            }

            switch (alt50) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:785:25: t= STRING
                    {
                    t=(Token)input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_argument_value1851); 
                     text = getString( t );  value=new ArgumentValueDescr(ArgumentValueDescr.STRING, text);

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:786:25: t= INT
                    {
                    t=(Token)input.LT(1);
                    match(input,INT,FOLLOW_INT_in_argument_value1862); 
                     text = t.getText();  value=new ArgumentValueDescr(ArgumentValueDescr.INTEGRAL, text);

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:787:25: t= FLOAT
                    {
                    t=(Token)input.LT(1);
                    match(input,FLOAT,FOLLOW_FLOAT_in_argument_value1875); 
                     text = t.getText(); value=new ArgumentValueDescr(ArgumentValueDescr.DECIMAL, text); 

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:788:25: t= BOOL
                    {
                    t=(Token)input.LT(1);
                    match(input,BOOL,FOLLOW_BOOL_in_argument_value1886); 
                     text = t.getText(); value=new ArgumentValueDescr(ArgumentValueDescr.BOOLEAN, text); 

                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:789:25: t= ID
                    {
                    t=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_argument_value1898); 
                     text = t.getText(); value=new ArgumentValueDescr(ArgumentValueDescr.VARIABLE, text);

                    }
                    break;
                case 6 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:790:25: t= 'null'
                    {
                    t=(Token)input.LT(1);
                    match(input,43,FOLLOW_43_in_argument_value1909); 
                     text = "null"; value=new ArgumentValueDescr(ArgumentValueDescr.NULL, text);

                    }
                    break;
                case 7 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:791:25: t= 'null'
                    {
                    t=(Token)input.LT(1);
                    match(input,43,FOLLOW_43_in_argument_value1920); 
                     text = "null"; value=new ArgumentValueDescr(ArgumentValueDescr.NULL, text);

                    }
                    break;
                case 8 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:792:25: m= inline_map
                    {
                    following.push(FOLLOW_inline_map_in_argument_value1939);
                    m=inline_map();
                    following.pop();

                      value=new ArgumentValueDescr(ArgumentValueDescr.MAP, m.getKeyValuePairs() ); 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return value;
    }
    // $ANTLR end argument_value


    // $ANTLR start inline_map
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:796:1: inline_map returns [ArgumentValueDescr.MapDescr mapDescr] : '{' (key= argument_value '=>' value= argument_value ) ( ( EOL )? ',' ( EOL )? key= argument_value '=>' value= argument_value )* '}' ;
    public ArgumentValueDescr.MapDescr inline_map() throws RecognitionException {   
        ArgumentValueDescr.MapDescr mapDescr;
        ArgumentValueDescr key = null;

        ArgumentValueDescr value = null;


        
                mapDescr = new ArgumentValueDescr.MapDescr();
            
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:800:8: ( '{' (key= argument_value '=>' value= argument_value ) ( ( EOL )? ',' ( EOL )? key= argument_value '=>' value= argument_value )* '}' )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:800:8: '{' (key= argument_value '=>' value= argument_value ) ( ( EOL )? ',' ( EOL )? key= argument_value '=>' value= argument_value )* '}'
            {
            match(input,26,FOLLOW_26_in_inline_map1979); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:801:12: (key= argument_value '=>' value= argument_value )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:801:14: key= argument_value '=>' value= argument_value
            {
            following.push(FOLLOW_argument_value_in_inline_map1997);
            key=argument_value();
            following.pop();

            match(input,44,FOLLOW_44_in_inline_map1999); 
            following.push(FOLLOW_argument_value_in_inline_map2003);
            value=argument_value();
            following.pop();

            
                             if ( key != null ) {
                                 mapDescr.add( new ArgumentValueDescr.KeyValuePairDescr( key, value ) );
                             }
                         

            }

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:12: ( ( EOL )? ',' ( EOL )? key= argument_value '=>' value= argument_value )*
            loop53:
            do {
                int alt53=2;
                int LA53_0 = input.LA(1);
                if ( LA53_0==EOL||LA53_0==24 ) {
                    alt53=1;
                }


                switch (alt53) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:14: ( EOL )? ',' ( EOL )? key= argument_value '=>' value= argument_value
            	    {
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:14: ( EOL )?
            	    int alt51=2;
            	    int LA51_0 = input.LA(1);
            	    if ( LA51_0==EOL ) {
            	        alt51=1;
            	    }
            	    else if ( LA51_0==24 ) {
            	        alt51=2;
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("808:14: ( EOL )?", 51, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt51) {
            	        case 1 :
            	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:15: EOL
            	            {
            	            match(input,EOL,FOLLOW_EOL_in_inline_map2046); 

            	            }
            	            break;

            	    }

            	    match(input,24,FOLLOW_24_in_inline_map2050); 
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:25: ( EOL )?
            	    int alt52=2;
            	    int LA52_0 = input.LA(1);
            	    if ( LA52_0==EOL ) {
            	        alt52=1;
            	    }
            	    else if ( (LA52_0>=ID && LA52_0<=FLOAT)||LA52_0==26||LA52_0==43 ) {
            	        alt52=2;
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("808:25: ( EOL )?", 52, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt52) {
            	        case 1 :
            	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:808:26: EOL
            	            {
            	            match(input,EOL,FOLLOW_EOL_in_inline_map2053); 

            	            }
            	            break;

            	    }

            	    following.push(FOLLOW_argument_value_in_inline_map2059);
            	    key=argument_value();
            	    following.pop();

            	    match(input,44,FOLLOW_44_in_inline_map2061); 
            	    following.push(FOLLOW_argument_value_in_inline_map2065);
            	    value=argument_value();
            	    following.pop();

            	    
            	                     if ( key != null ) {
            	                         mapDescr.add( new ArgumentValueDescr.KeyValuePairDescr( key, value ) );
            	                     }
            	                 

            	    }
            	    break;

            	default :
            	    break loop53;
                }
            } while (true);

            match(input,27,FOLLOW_27_in_inline_map2101); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return mapDescr;
    }
    // $ANTLR end inline_map


    // $ANTLR start fact_binding
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:817:1: fact_binding returns [PatternDescr d] : id= ID opt_eol ':' opt_eol fe= fact_expression[id.getText()] ;
    public PatternDescr fact_binding() throws RecognitionException {   
        PatternDescr d;
        Token id=null;
        PatternDescr fe = null;


        
        		d=null;
        		boolean multi=false;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:823:17: (id= ID opt_eol ':' opt_eol fe= fact_expression[id.getText()] )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:823:17: id= ID opt_eol ':' opt_eol fe= fact_expression[id.getText()]
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_fact_binding2133); 
            following.push(FOLLOW_opt_eol_in_fact_binding2143);
            opt_eol();
            following.pop();

            match(input,33,FOLLOW_33_in_fact_binding2145); 
            following.push(FOLLOW_opt_eol_in_fact_binding2147);
            opt_eol();
            following.pop();

            following.push(FOLLOW_fact_expression_in_fact_binding2151);
            fe=fact_expression(id.getText());
            following.pop();

            
             			d=fe;
             		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end fact_binding


    // $ANTLR start fact_expression
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:831:2: fact_expression[String id] returns [PatternDescr pd] : ( '(' opt_eol fe= fact_expression[id] opt_eol ')' | f= fact opt_eol ( ('or'|'||') opt_eol f= fact )* );
    public PatternDescr fact_expression(String id) throws RecognitionException {   
        PatternDescr pd;
        PatternDescr fe = null;

        PatternDescr f = null;


        
         		pd = null;
         		boolean multi = false;
         	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:836:17: ( '(' opt_eol fe= fact_expression[id] opt_eol ')' | f= fact opt_eol ( ('or'|'||') opt_eol f= fact )* )
            int alt55=2;
            int LA55_0 = input.LA(1);
            if ( LA55_0==23 ) {
                alt55=1;
            }
            else if ( LA55_0==ID ) {
                alt55=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("831:2: fact_expression[String id] returns [PatternDescr pd] : ( \'(\' opt_eol fe= fact_expression[id] opt_eol \')\' | f= fact opt_eol ( (\'or\'|\'||\') opt_eol f= fact )* );", 55, 0, input);

                throw nvae;
            }
            switch (alt55) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:836:17: '(' opt_eol fe= fact_expression[id] opt_eol ')'
                    {
                    match(input,23,FOLLOW_23_in_fact_expression2183); 
                    following.push(FOLLOW_opt_eol_in_fact_expression2185);
                    opt_eol();
                    following.pop();

                    following.push(FOLLOW_fact_expression_in_fact_expression2189);
                    fe=fact_expression(id);
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_fact_expression2191);
                    opt_eol();
                    following.pop();

                    match(input,25,FOLLOW_25_in_fact_expression2193); 
                     pd=fe; 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:837:17: f= fact opt_eol ( ('or'|'||') opt_eol f= fact )*
                    {
                    following.push(FOLLOW_fact_in_fact_expression2204);
                    f=fact();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_fact_expression2206);
                    opt_eol();
                    following.pop();

                    
                     			((ColumnDescr)f).setIdentifier( id );
                     			pd = f;
                     		
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:842:17: ( ('or'|'||') opt_eol f= fact )*
                    loop54:
                    do {
                        int alt54=2;
                        int LA54_0 = input.LA(1);
                        if ( (LA54_0>=45 && LA54_0<=46) ) {
                            alt54=1;
                        }


                        switch (alt54) {
                    	case 1 :
                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:842:25: ('or'|'||') opt_eol f= fact
                    	    {
                    	    if ( (input.LA(1)>=45 && input.LA(1)<=46) ) {
                    	        input.consume();
                    	        errorRecovery=false;
                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_fact_expression2219);    throw mse;
                    	    }

                    	    following.push(FOLLOW_opt_eol_in_fact_expression2224);
                    	    opt_eol();
                    	    following.pop();

                    	    	if ( ! multi ) {
                    	     					PatternDescr first = pd;
                    	     					pd = new OrDescr();
                    	     					((OrDescr)pd).addDescr( first );
                    	     					multi=true;
                    	     				}
                    	     			
                    	    following.push(FOLLOW_fact_in_fact_expression2238);
                    	    f=fact();
                    	    following.pop();

                    	    
                    	     				((ColumnDescr)f).setIdentifier( id );
                    	     				((OrDescr)pd).addDescr( f );
                    	     			

                    	    }
                    	    break;

                    	default :
                    	    break loop54;
                        }
                    } while (true);


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return pd;
    }
    // $ANTLR end fact_expression


    // $ANTLR start fact
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:858:1: fact returns [PatternDescr d] : id= dotted_name opt_eol loc= '(' opt_eol (c= constraints )? opt_eol endLoc= ')' opt_eol ;
    public PatternDescr fact() throws RecognitionException {   
        PatternDescr d;
        Token loc=null;
        Token endLoc=null;
        String id = null;

        List c = null;


        
        		d=null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:862:17: (id= dotted_name opt_eol loc= '(' opt_eol (c= constraints )? opt_eol endLoc= ')' opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:862:17: id= dotted_name opt_eol loc= '(' opt_eol (c= constraints )? opt_eol endLoc= ')' opt_eol
            {
            following.push(FOLLOW_dotted_name_in_fact2277);
            id=dotted_name();
            following.pop();

             
             			d = new ColumnDescr( id ); 
             		
            following.push(FOLLOW_opt_eol_in_fact2285);
            opt_eol();
            following.pop();

            loc=(Token)input.LT(1);
            match(input,23,FOLLOW_23_in_fact2293); 
            
             				d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
             			
            following.push(FOLLOW_opt_eol_in_fact2296);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:868:34: (c= constraints )?
            int alt56=2;
            alt56 = dfa56.predict(input); 
            switch (alt56) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:868:41: c= constraints
                    {
                    following.push(FOLLOW_constraints_in_fact2302);
                    c=constraints();
                    following.pop();

                    
                    		 			for ( Iterator cIter = c.iterator() ; cIter.hasNext() ; ) {
                     						((ColumnDescr)d).addDescr( (PatternDescr) cIter.next() );
                     					}
                     				

                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_fact2321);
            opt_eol();
            following.pop();

            endLoc=(Token)input.LT(1);
            match(input,25,FOLLOW_25_in_fact2325); 
            following.push(FOLLOW_opt_eol_in_fact2327);
            opt_eol();
            following.pop();

            
             					d.setEndLocation( offset(endLoc.getLine()), endLoc.getCharPositionInLine() );	
             				

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end fact


    // $ANTLR start constraints
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:882:1: constraints returns [List constraints] : opt_eol ( constraint[constraints] | predicate[constraints] ) ( opt_eol ',' opt_eol ( constraint[constraints] | predicate[constraints] ) )* opt_eol ;
    public List constraints() throws RecognitionException {   
        List constraints;
        
        		constraints = new ArrayList();
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:886:17: ( opt_eol ( constraint[constraints] | predicate[constraints] ) ( opt_eol ',' opt_eol ( constraint[constraints] | predicate[constraints] ) )* opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:886:17: opt_eol ( constraint[constraints] | predicate[constraints] ) ( opt_eol ',' opt_eol ( constraint[constraints] | predicate[constraints] ) )* opt_eol
            {
            following.push(FOLLOW_opt_eol_in_constraints2359);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:887:17: ( constraint[constraints] | predicate[constraints] )
            int alt57=2;
            int LA57_0 = input.LA(1);
            if ( LA57_0==EOL||LA57_0==15 ) {
                alt57=1;
            }
            else if ( LA57_0==ID ) {
                int LA57_2 = input.LA(2);
                if ( LA57_2==33 ) {
                    int LA57_3 = input.LA(3);
                    if ( LA57_3==ID ) {
                        int LA57_17 = input.LA(4);
                        if ( LA57_17==49 ) {
                            alt57=2;
                        }
                        else if ( LA57_17==EOL||LA57_17==15||(LA57_17>=24 && LA57_17<=25)||(LA57_17>=58 && LA57_17<=67) ) {
                            alt57=1;
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("887:17: ( constraint[constraints] | predicate[constraints] )", 57, 17, input);

                            throw nvae;
                        }
                    }
                    else if ( LA57_3==EOL||LA57_3==15 ) {
                        alt57=1;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("887:17: ( constraint[constraints] | predicate[constraints] )", 57, 3, input);

                        throw nvae;
                    }
                }
                else if ( LA57_2==EOL||LA57_2==15||(LA57_2>=24 && LA57_2<=25)||(LA57_2>=58 && LA57_2<=67) ) {
                    alt57=1;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("887:17: ( constraint[constraints] | predicate[constraints] )", 57, 2, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("887:17: ( constraint[constraints] | predicate[constraints] )", 57, 0, input);

                throw nvae;
            }
            switch (alt57) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:887:18: constraint[constraints]
                    {
                    following.push(FOLLOW_constraint_in_constraints2364);
                    constraint(constraints);
                    following.pop();


                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:887:42: predicate[constraints]
                    {
                    following.push(FOLLOW_predicate_in_constraints2367);
                    predicate(constraints);
                    following.pop();


                    }
                    break;

            }

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:888:17: ( opt_eol ',' opt_eol ( constraint[constraints] | predicate[constraints] ) )*
            loop59:
            do {
                int alt59=2;
                alt59 = dfa59.predict(input); 
                switch (alt59) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:888:19: opt_eol ',' opt_eol ( constraint[constraints] | predicate[constraints] )
            	    {
            	    following.push(FOLLOW_opt_eol_in_constraints2375);
            	    opt_eol();
            	    following.pop();

            	    match(input,24,FOLLOW_24_in_constraints2377); 
            	    following.push(FOLLOW_opt_eol_in_constraints2379);
            	    opt_eol();
            	    following.pop();

            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:888:39: ( constraint[constraints] | predicate[constraints] )
            	    int alt58=2;
            	    int LA58_0 = input.LA(1);
            	    if ( LA58_0==EOL||LA58_0==15 ) {
            	        alt58=1;
            	    }
            	    else if ( LA58_0==ID ) {
            	        int LA58_2 = input.LA(2);
            	        if ( LA58_2==33 ) {
            	            int LA58_3 = input.LA(3);
            	            if ( LA58_3==ID ) {
            	                int LA58_17 = input.LA(4);
            	                if ( LA58_17==49 ) {
            	                    alt58=2;
            	                }
            	                else if ( LA58_17==EOL||LA58_17==15||(LA58_17>=24 && LA58_17<=25)||(LA58_17>=58 && LA58_17<=67) ) {
            	                    alt58=1;
            	                }
            	                else {
            	                    NoViableAltException nvae =
            	                        new NoViableAltException("888:39: ( constraint[constraints] | predicate[constraints] )", 58, 17, input);

            	                    throw nvae;
            	                }
            	            }
            	            else if ( LA58_3==EOL||LA58_3==15 ) {
            	                alt58=1;
            	            }
            	            else {
            	                NoViableAltException nvae =
            	                    new NoViableAltException("888:39: ( constraint[constraints] | predicate[constraints] )", 58, 3, input);

            	                throw nvae;
            	            }
            	        }
            	        else if ( LA58_2==EOL||LA58_2==15||(LA58_2>=24 && LA58_2<=25)||(LA58_2>=58 && LA58_2<=67) ) {
            	            alt58=1;
            	        }
            	        else {
            	            NoViableAltException nvae =
            	                new NoViableAltException("888:39: ( constraint[constraints] | predicate[constraints] )", 58, 2, input);

            	            throw nvae;
            	        }
            	    }
            	    else {
            	        NoViableAltException nvae =
            	            new NoViableAltException("888:39: ( constraint[constraints] | predicate[constraints] )", 58, 0, input);

            	        throw nvae;
            	    }
            	    switch (alt58) {
            	        case 1 :
            	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:888:40: constraint[constraints]
            	            {
            	            following.push(FOLLOW_constraint_in_constraints2382);
            	            constraint(constraints);
            	            following.pop();


            	            }
            	            break;
            	        case 2 :
            	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:888:64: predicate[constraints]
            	            {
            	            following.push(FOLLOW_predicate_in_constraints2385);
            	            predicate(constraints);
            	            following.pop();


            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop59;
                }
            } while (true);

            following.push(FOLLOW_opt_eol_in_constraints2393);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return constraints;
    }
    // $ANTLR end constraints


    // $ANTLR start constraint
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:892:1: constraint[List constraints] : opt_eol (fb= ID opt_eol ':' opt_eol )? f= ID opt_eol (op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )* )? opt_eol ;
    public void constraint(List constraints) throws RecognitionException {   
        Token fb=null;
        Token f=null;
        Token bvc=null;
        Token con=null;
        String op = null;

        String lc = null;

        String rvc = null;


        
        		PatternDescr d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:896:17: ( opt_eol (fb= ID opt_eol ':' opt_eol )? f= ID opt_eol (op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )* )? opt_eol )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:896:17: opt_eol (fb= ID opt_eol ':' opt_eol )? f= ID opt_eol (op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )* )? opt_eol
            {
            following.push(FOLLOW_opt_eol_in_constraint2412);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:897:17: (fb= ID opt_eol ':' opt_eol )?
            int alt60=2;
            alt60 = dfa60.predict(input); 
            switch (alt60) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:897:19: fb= ID opt_eol ':' opt_eol
                    {
                    fb=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_constraint2420); 
                    following.push(FOLLOW_opt_eol_in_constraint2422);
                    opt_eol();
                    following.pop();

                    match(input,33,FOLLOW_33_in_constraint2424); 
                    following.push(FOLLOW_opt_eol_in_constraint2426);
                    opt_eol();
                    following.pop();


                    }
                    break;

            }

            f=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_constraint2436); 
            
            
            			if ( fb != null ) {
            				//System.err.println( "fb: " + fb.getText() );
            				//System.err.println( " f: " + f.getText() );
            				d = new FieldBindingDescr( f.getText(), fb.getText() );
            				//System.err.println( "fbd: " + d );
            				
            				d.setLocation( offset(f.getLine()), f.getCharPositionInLine() );
            				constraints.add( d );
            			} 
            			FieldConstraintDescr fc = new FieldConstraintDescr(f.getText());
            			fc.setLocation( offset(f.getLine()), f.getCharPositionInLine() );
            									
            			
            		
            following.push(FOLLOW_opt_eol_in_constraint2450);
            opt_eol();
            following.pop();

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:915:33: (op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )* )?
            int alt64=2;
            int LA64_0 = input.LA(1);
            if ( (LA64_0>=58 && LA64_0<=67) ) {
                alt64=1;
            }
            else if ( LA64_0==EOL||LA64_0==15||(LA64_0>=24 && LA64_0<=25) ) {
                alt64=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("915:33: (op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= (\'&\'|\'|\')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )* )?", 64, 0, input);

                throw nvae;
            }
            switch (alt64) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:915:41: op= operator opt_eol (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )*
                    {
                    following.push(FOLLOW_operator_in_constraint2456);
                    op=operator();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_constraint2458);
                    opt_eol();
                    following.pop();

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:917:41: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )
                    int alt61=4;
                    switch ( input.LA(1) ) {
                    case ID:
                        int LA61_1 = input.LA(2);
                        if ( LA61_1==18 ) {
                            alt61=2;
                        }
                        else if ( LA61_1==EOL||LA61_1==15||(LA61_1>=24 && LA61_1<=25)||(LA61_1>=47 && LA61_1<=48) ) {
                            alt61=1;
                        }
                        else {
                            NoViableAltException nvae =
                                new NoViableAltException("917:41: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )", 61, 1, input);

                            throw nvae;
                        }
                        break;
                    case INT:
                    case BOOL:
                    case STRING:
                    case FLOAT:
                    case 43:
                        alt61=3;
                        break;
                    case 23:
                        alt61=4;
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("917:41: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )", 61, 0, input);

                        throw nvae;
                    }

                    switch (alt61) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:917:49: bvc= ID
                            {
                            bvc=(Token)input.LT(1);
                            match(input,ID,FOLLOW_ID_in_constraint2476); 
                            
                            							
                            														
                            							
                            							VariableRestrictionDescr vd = new VariableRestrictionDescr(op, bvc.getText());
                            							fc.addRestriction(vd);
                            							constraints.add(fc);
                            							
                            						

                            }
                            break;
                        case 2 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:928:49: lc= enum_constraint
                            {
                            following.push(FOLLOW_enum_constraint_in_constraint2501);
                            lc=enum_constraint();
                            following.pop();

                             
                            
                            							LiteralRestrictionDescr lrd  = new LiteralRestrictionDescr(op, lc, true);
                            							fc.addRestriction(lrd);
                            							constraints.add(fc);
                            							
                            						

                            }
                            break;
                        case 3 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:937:49: lc= literal_constraint
                            {
                            following.push(FOLLOW_literal_constraint_in_constraint2533);
                            lc=literal_constraint();
                            following.pop();

                             
                            							
                            							LiteralRestrictionDescr lrd  = new LiteralRestrictionDescr(op, lc);
                            							fc.addRestriction(lrd);
                            							constraints.add(fc);
                            							
                            						

                            }
                            break;
                        case 4 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:945:49: rvc= retval_constraint
                            {
                            following.push(FOLLOW_retval_constraint_in_constraint2553);
                            rvc=retval_constraint();
                            following.pop();

                             
                            							
                            							
                            
                            							ReturnValueRestrictionDescr rvd = new ReturnValueRestrictionDescr(op, rvc);							
                            							fc.addRestriction(rvd);
                            							constraints.add(fc);
                            							
                            						

                            }
                            break;

                    }

                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:956:41: (con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint ) )*
                    loop63:
                    do {
                        int alt63=2;
                        int LA63_0 = input.LA(1);
                        if ( (LA63_0>=47 && LA63_0<=48) ) {
                            alt63=1;
                        }


                        switch (alt63) {
                    	case 1 :
                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:957:49: con= ('&'|'|')op= operator (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )
                    	    {
                    	    con=(Token)input.LT(1);
                    	    if ( (input.LA(1)>=47 && input.LA(1)<=48) ) {
                    	        input.consume();
                    	        errorRecovery=false;
                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_constraint2588);    throw mse;
                    	    }

                    	    
                    	    							if (con.getText().equals("&") ) {								
                    	    								fc.addRestriction(new RestrictionConnectiveDescr(RestrictionConnectiveDescr.AND));	
                    	    							} else {
                    	    								fc.addRestriction(new RestrictionConnectiveDescr(RestrictionConnectiveDescr.OR));	
                    	    							}							
                    	    						
                    	    following.push(FOLLOW_operator_in_constraint2610);
                    	    op=operator();
                    	    following.pop();

                    	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:967:49: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )
                    	    int alt62=4;
                    	    switch ( input.LA(1) ) {
                    	    case ID:
                    	        int LA62_1 = input.LA(2);
                    	        if ( LA62_1==18 ) {
                    	            alt62=2;
                    	        }
                    	        else if ( LA62_1==EOL||LA62_1==15||(LA62_1>=24 && LA62_1<=25)||(LA62_1>=47 && LA62_1<=48) ) {
                    	            alt62=1;
                    	        }
                    	        else {
                    	            NoViableAltException nvae =
                    	                new NoViableAltException("967:49: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )", 62, 1, input);

                    	            throw nvae;
                    	        }
                    	        break;
                    	    case INT:
                    	    case BOOL:
                    	    case STRING:
                    	    case FLOAT:
                    	    case 43:
                    	        alt62=3;
                    	        break;
                    	    case 23:
                    	        alt62=4;
                    	        break;
                    	    default:
                    	        NoViableAltException nvae =
                    	            new NoViableAltException("967:49: (bvc= ID | lc= enum_constraint | lc= literal_constraint | rvc= retval_constraint )", 62, 0, input);

                    	        throw nvae;
                    	    }

                    	    switch (alt62) {
                    	        case 1 :
                    	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:967:57: bvc= ID
                    	            {
                    	            bvc=(Token)input.LT(1);
                    	            match(input,ID,FOLLOW_ID_in_constraint2622); 
                    	            
                    	            								VariableRestrictionDescr vd = new VariableRestrictionDescr(op, bvc.getText());
                    	            								fc.addRestriction(vd);
                    	            							

                    	            }
                    	            break;
                    	        case 2 :
                    	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:973:57: lc= enum_constraint
                    	            {
                    	            following.push(FOLLOW_enum_constraint_in_constraint2650);
                    	            lc=enum_constraint();
                    	            following.pop();

                    	             
                    	            								LiteralRestrictionDescr lrd  = new LiteralRestrictionDescr(op, lc, true);
                    	            								fc.addRestriction(lrd);
                    	            								
                    	            							

                    	            }
                    	            break;
                    	        case 3 :
                    	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:980:57: lc= literal_constraint
                    	            {
                    	            following.push(FOLLOW_literal_constraint_in_constraint2685);
                    	            lc=literal_constraint();
                    	            following.pop();

                    	             
                    	            								LiteralRestrictionDescr lrd  = new LiteralRestrictionDescr(op, lc);
                    	            								fc.addRestriction(lrd);
                    	            								
                    	            							

                    	            }
                    	            break;
                    	        case 4 :
                    	            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:986:57: rvc= retval_constraint
                    	            {
                    	            following.push(FOLLOW_retval_constraint_in_constraint2707);
                    	            rvc=retval_constraint();
                    	            following.pop();

                    	             
                    	            								ReturnValueRestrictionDescr rvd = new ReturnValueRestrictionDescr(op, rvc);							
                    	            								fc.addRestriction(rvd);
                    	            								
                    	            							

                    	            }
                    	            break;

                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop63;
                        }
                    } while (true);


                    }
                    break;

            }

            following.push(FOLLOW_opt_eol_in_constraint2763);
            opt_eol();
            following.pop();


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end constraint


    // $ANTLR start literal_constraint
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:999:1: literal_constraint returns [String text] : (t= STRING | t= INT | t= FLOAT | t= BOOL | t= 'null' ) ;
    public String literal_constraint() throws RecognitionException {   
        String text;
        Token t=null;

        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1003:17: ( (t= STRING | t= INT | t= FLOAT | t= BOOL | t= 'null' ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1003:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= 'null' )
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1003:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= 'null' )
            int alt65=5;
            switch ( input.LA(1) ) {
            case STRING:
                alt65=1;
                break;
            case INT:
                alt65=2;
                break;
            case FLOAT:
                alt65=3;
                break;
            case BOOL:
                alt65=4;
                break;
            case 43:
                alt65=5;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1003:17: (t= STRING | t= INT | t= FLOAT | t= BOOL | t= \'null\' )", 65, 0, input);

                throw nvae;
            }

            switch (alt65) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1003:25: t= STRING
                    {
                    t=(Token)input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_literal_constraint2790); 
                     text = getString( t ); 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1004:25: t= INT
                    {
                    t=(Token)input.LT(1);
                    match(input,INT,FOLLOW_INT_in_literal_constraint2801); 
                     text = t.getText(); 

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1005:25: t= FLOAT
                    {
                    t=(Token)input.LT(1);
                    match(input,FLOAT,FOLLOW_FLOAT_in_literal_constraint2814); 
                     text = t.getText(); 

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1006:25: t= BOOL
                    {
                    t=(Token)input.LT(1);
                    match(input,BOOL,FOLLOW_BOOL_in_literal_constraint2825); 
                     text = t.getText(); 

                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1007:25: t= 'null'
                    {
                    t=(Token)input.LT(1);
                    match(input,43,FOLLOW_43_in_literal_constraint2837); 
                     text = null; 

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end literal_constraint


    // $ANTLR start enum_constraint
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1011:1: enum_constraint returns [String text] : (cls= ID '.' en= ID ) ;
    public String enum_constraint() throws RecognitionException {   
        String text;
        Token cls=null;
        Token en=null;

        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1015:17: ( (cls= ID '.' en= ID ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1015:17: (cls= ID '.' en= ID )
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1015:17: (cls= ID '.' en= ID )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1015:18: cls= ID '.' en= ID
            {
            cls=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_enum_constraint2868); 
            match(input,18,FOLLOW_18_in_enum_constraint2870); 
            en=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_enum_constraint2874); 

            }

             text = cls.getText() + "." + en.getText(); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end enum_constraint


    // $ANTLR start retval_constraint
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1018:1: retval_constraint returns [String text] : '(' c= paren_chunk ')' ;
    public String retval_constraint() throws RecognitionException {   
        String text;
        String c = null;


        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1023:17: ( '(' c= paren_chunk ')' )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1023:17: '(' c= paren_chunk ')'
            {
            match(input,23,FOLLOW_23_in_retval_constraint2903); 
            following.push(FOLLOW_paren_chunk_in_retval_constraint2908);
            c=paren_chunk();
            following.pop();

            match(input,25,FOLLOW_25_in_retval_constraint2911); 
             text = c; 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end retval_constraint


    // $ANTLR start predicate
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1026:1: predicate[List constraints] : decl= ID ':' field= ID '->' '(' text= paren_chunk ')' ;
    public void predicate(List constraints) throws RecognitionException {   
        Token decl=null;
        Token field=null;
        String text = null;


        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1028:17: (decl= ID ':' field= ID '->' '(' text= paren_chunk ')' )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1028:17: decl= ID ':' field= ID '->' '(' text= paren_chunk ')'
            {
            decl=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_predicate2929); 
            match(input,33,FOLLOW_33_in_predicate2931); 
            field=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_predicate2935); 
            match(input,49,FOLLOW_49_in_predicate2937); 
            match(input,23,FOLLOW_23_in_predicate2939); 
            following.push(FOLLOW_paren_chunk_in_predicate2943);
            text=paren_chunk();
            following.pop();

            match(input,25,FOLLOW_25_in_predicate2945); 
            
            			PredicateDescr d = new PredicateDescr(field.getText(), decl.getText(), text );
            			constraints.add( d );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end predicate


    // $ANTLR start paren_chunk
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1035:1: paren_chunk returns [String text] : ( options {greedy=false; } : '(' c= paren_chunk ')' | any= . )* ;
    public String paren_chunk() throws RecognitionException {   
        String text;
        Token any=null;
        String c = null;


        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1041:18: ( ( options {greedy=false; } : '(' c= paren_chunk ')' | any= . )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1041:18: ( options {greedy=false; } : '(' c= paren_chunk ')' | any= . )*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1041:18: ( options {greedy=false; } : '(' c= paren_chunk ')' | any= . )*
            loop66:
            do {
                int alt66=3;
                switch ( input.LA(1) ) {
                case EOL:
                    alt66=3;
                    break;
                case 25:
                    alt66=3;
                    break;
                case 23:
                    alt66=1;
                    break;
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 24:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    alt66=2;
                    break;

                }

                switch (alt66) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1042:25: '(' c= paren_chunk ')'
            	    {
            	    match(input,23,FOLLOW_23_in_paren_chunk2991); 
            	    following.push(FOLLOW_paren_chunk_in_paren_chunk2995);
            	    c=paren_chunk();
            	    following.pop();

            	    match(input,25,FOLLOW_25_in_paren_chunk2997); 
            	    
            	    				if ( c == null ) {
            	    					c = "";
            	    				}
            	    				if ( text == null ) {
            	    					text = "( " + c + " )";
            	    				} else {
            	    					text = text + " ( " + c + " )";
            	    				}
            	    			

            	    }
            	    break;
            	case 2 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1053:19: any= .
            	    {
            	    any=(Token)input.LT(1);
            	    matchAny(input); 
            	    
            	    				if ( text == null ) {
            	    					text = any.getText();
            	    				} else {
            	    					text = text + " " + any.getText(); 
            	    				} 
            	    			

            	    }
            	    break;

            	default :
            	    break loop66;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end paren_chunk


    // $ANTLR start paren_chunk2
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1065:1: paren_chunk2 returns [String text] : ( options {greedy=false; } : '(' c= paren_chunk2 ')' | any= . )* ;
    public String paren_chunk2() throws RecognitionException {   
        String text;
        Token any=null;
        String c = null;


        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1071:18: ( ( options {greedy=false; } : '(' c= paren_chunk2 ')' | any= . )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1071:18: ( options {greedy=false; } : '(' c= paren_chunk2 ')' | any= . )*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1071:18: ( options {greedy=false; } : '(' c= paren_chunk2 ')' | any= . )*
            loop67:
            do {
                int alt67=3;
                switch ( input.LA(1) ) {
                case 25:
                    alt67=3;
                    break;
                case 23:
                    alt67=1;
                    break;
                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 24:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    alt67=2;
                    break;

                }

                switch (alt67) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1072:25: '(' c= paren_chunk2 ')'
            	    {
            	    match(input,23,FOLLOW_23_in_paren_chunk23068); 
            	    following.push(FOLLOW_paren_chunk2_in_paren_chunk23072);
            	    c=paren_chunk2();
            	    following.pop();

            	    match(input,25,FOLLOW_25_in_paren_chunk23074); 
            	    
            	    				if ( c == null ) {
            	    					c = "";
            	    				}
            	    				if ( text == null ) {
            	    					text = "( " + c + " )";
            	    				} else {
            	    					text = text + " ( " + c + " )";
            	    				}
            	    			

            	    }
            	    break;
            	case 2 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1083:19: any= .
            	    {
            	    any=(Token)input.LT(1);
            	    matchAny(input); 
            	    
            	    				if ( text == null ) {
            	    					text = any.getText();
            	    				} else {
            	    					text = text + " " + any.getText(); 
            	    				} 
            	    			

            	    }
            	    break;

            	default :
            	    break loop67;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end paren_chunk2


    // $ANTLR start curly_chunk
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1094:1: curly_chunk returns [String text] : ( options {greedy=false; } : '{' c= curly_chunk '}' | any= . )* ;
    public String curly_chunk() throws RecognitionException {   
        String text;
        Token any=null;
        String c = null;


        
        		text = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1100:17: ( ( options {greedy=false; } : '{' c= curly_chunk '}' | any= . )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1100:17: ( options {greedy=false; } : '{' c= curly_chunk '}' | any= . )*
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1100:17: ( options {greedy=false; } : '{' c= curly_chunk '}' | any= . )*
            loop68:
            do {
                int alt68=3;
                switch ( input.LA(1) ) {
                case 27:
                    alt68=3;
                    break;
                case 26:
                    alt68=1;
                    break;
                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    alt68=2;
                    break;

                }

                switch (alt68) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1101:25: '{' c= curly_chunk '}'
            	    {
            	    match(input,26,FOLLOW_26_in_curly_chunk3143); 
            	    following.push(FOLLOW_curly_chunk_in_curly_chunk3147);
            	    c=curly_chunk();
            	    following.pop();

            	    match(input,27,FOLLOW_27_in_curly_chunk3149); 
            	    
            	    				//System.err.println( "chunk [" + c + "]" );
            	    				if ( c == null ) {
            	    					c = "";
            	    				}
            	    				if ( text == null ) {
            	    					text = "{ " + c + " }";
            	    				} else {
            	    					text = text + " { " + c + " }";
            	    				}
            	    			

            	    }
            	    break;
            	case 2 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1113:19: any= .
            	    {
            	    any=(Token)input.LT(1);
            	    matchAny(input); 
            	    
            	    				//System.err.println( "any [" + any.getText() + "]" );
            	    				if ( text == null ) {
            	    					text = any.getText();
            	    				} else {
            	    					text = text + " " + any.getText(); 
            	    				} 
            	    			

            	    }
            	    break;

            	default :
            	    break loop68;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return text;
    }
    // $ANTLR end curly_chunk


    // $ANTLR start lhs_or
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1125:1: lhs_or returns [PatternDescr d] : left= lhs_and ( ('or'|'||') opt_eol right= lhs_and )* ;
    public PatternDescr lhs_or() throws RecognitionException {   
        PatternDescr d;
        PatternDescr left = null;

        PatternDescr right = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1130:17: (left= lhs_and ( ('or'|'||') opt_eol right= lhs_and )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1130:17: left= lhs_and ( ('or'|'||') opt_eol right= lhs_and )*
            {
             OrDescr or = null; 
            following.push(FOLLOW_lhs_and_in_lhs_or3207);
            left=lhs_and();
            following.pop();

            d = left; 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1132:17: ( ('or'|'||') opt_eol right= lhs_and )*
            loop69:
            do {
                int alt69=2;
                int LA69_0 = input.LA(1);
                if ( (LA69_0>=45 && LA69_0<=46) ) {
                    alt69=1;
                }


                switch (alt69) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1132:19: ('or'|'||') opt_eol right= lhs_and
            	    {
            	    if ( (input.LA(1)>=45 && input.LA(1)<=46) ) {
            	        input.consume();
            	        errorRecovery=false;
            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_lhs_or3216);    throw mse;
            	    }

            	    following.push(FOLLOW_opt_eol_in_lhs_or3221);
            	    opt_eol();
            	    following.pop();

            	    following.push(FOLLOW_lhs_and_in_lhs_or3228);
            	    right=lhs_and();
            	    following.pop();

            	    
            	    				if ( or == null ) {
            	    					or = new OrDescr();
            	    					or.addDescr( left );
            	    					d = or;
            	    				}
            	    				
            	    				or.addDescr( right );
            	    			

            	    }
            	    break;

            	default :
            	    break loop69;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_or


    // $ANTLR start lhs_and
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1146:1: lhs_and returns [PatternDescr d] : left= lhs_unary ( ('and'|'&&') opt_eol right= lhs_unary )* ;
    public PatternDescr lhs_and() throws RecognitionException {   
        PatternDescr d;
        PatternDescr left = null;

        PatternDescr right = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1151:17: (left= lhs_unary ( ('and'|'&&') opt_eol right= lhs_unary )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1151:17: left= lhs_unary ( ('and'|'&&') opt_eol right= lhs_unary )*
            {
             AndDescr and = null; 
            following.push(FOLLOW_lhs_unary_in_lhs_and3268);
            left=lhs_unary();
            following.pop();

             d = left; 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1153:17: ( ('and'|'&&') opt_eol right= lhs_unary )*
            loop70:
            do {
                int alt70=2;
                int LA70_0 = input.LA(1);
                if ( (LA70_0>=50 && LA70_0<=51) ) {
                    alt70=1;
                }


                switch (alt70) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1153:19: ('and'|'&&') opt_eol right= lhs_unary
            	    {
            	    if ( (input.LA(1)>=50 && input.LA(1)<=51) ) {
            	        input.consume();
            	        errorRecovery=false;
            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recoverFromMismatchedSet(input,mse,FOLLOW_set_in_lhs_and3277);    throw mse;
            	    }

            	    following.push(FOLLOW_opt_eol_in_lhs_and3282);
            	    opt_eol();
            	    following.pop();

            	    following.push(FOLLOW_lhs_unary_in_lhs_and3289);
            	    right=lhs_unary();
            	    following.pop();

            	    
            	    				if ( and == null ) {
            	    					and = new AndDescr();
            	    					and.addDescr( left );
            	    					d = and;
            	    				}
            	    				
            	    				and.addDescr( right );
            	    			

            	    }
            	    break;

            	default :
            	    break loop70;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_and


    // $ANTLR start lhs_unary
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1167:1: lhs_unary returns [PatternDescr d] : (u= lhs_exist | u= lhs_not | u= lhs_eval | u= lhs_column (fm= from_statement )? | '(' opt_eol u= lhs opt_eol ')' ) ;
    public PatternDescr lhs_unary() throws RecognitionException {   
        PatternDescr d;
        PatternDescr u = null;

        FromDescr fm = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1171:17: ( (u= lhs_exist | u= lhs_not | u= lhs_eval | u= lhs_column (fm= from_statement )? | '(' opt_eol u= lhs opt_eol ')' ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1171:17: (u= lhs_exist | u= lhs_not | u= lhs_eval | u= lhs_column (fm= from_statement )? | '(' opt_eol u= lhs opt_eol ')' )
            {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1171:17: (u= lhs_exist | u= lhs_not | u= lhs_eval | u= lhs_column (fm= from_statement )? | '(' opt_eol u= lhs opt_eol ')' )
            int alt72=5;
            switch ( input.LA(1) ) {
            case 52:
                alt72=1;
                break;
            case 53:
                alt72=2;
                break;
            case 54:
                alt72=3;
                break;
            case ID:
                alt72=4;
                break;
            case 23:
                alt72=5;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1171:17: (u= lhs_exist | u= lhs_not | u= lhs_eval | u= lhs_column (fm= from_statement )? | \'(\' opt_eol u= lhs opt_eol \')\' )", 72, 0, input);

                throw nvae;
            }

            switch (alt72) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1171:25: u= lhs_exist
                    {
                    following.push(FOLLOW_lhs_exist_in_lhs_unary3327);
                    u=lhs_exist();
                    following.pop();

                    d = u;

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1172:25: u= lhs_not
                    {
                    following.push(FOLLOW_lhs_not_in_lhs_unary3337);
                    u=lhs_not();
                    following.pop();

                    d = u;

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1173:25: u= lhs_eval
                    {
                    following.push(FOLLOW_lhs_eval_in_lhs_unary3347);
                    u=lhs_eval();
                    following.pop();

                    d = u;

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1174:25: u= lhs_column (fm= from_statement )?
                    {
                    following.push(FOLLOW_lhs_column_in_lhs_unary3361);
                    u=lhs_column();
                    following.pop();

                    d=u;
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1174:45: (fm= from_statement )?
                    int alt71=2;
                    int LA71_0 = input.LA(1);
                    if ( LA71_0==42 ) {
                        alt71=1;
                    }
                    else if ( (LA71_0>=EOL && LA71_0<=ID)||LA71_0==15||LA71_0==23||LA71_0==25||LA71_0==29||LA71_0==34||(LA71_0>=45 && LA71_0<=46)||(LA71_0>=50 && LA71_0<=54) ) {
                        alt71=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("1174:45: (fm= from_statement )?", 71, 0, input);

                        throw nvae;
                    }
                    switch (alt71) {
                        case 1 :
                            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1174:46: fm= from_statement
                            {
                            following.push(FOLLOW_from_statement_in_lhs_unary3368);
                            fm=from_statement();
                            following.pop();

                            fm.setColumn((ColumnDescr) u); d=fm;

                            }
                            break;

                    }


                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1175:25: '(' opt_eol u= lhs opt_eol ')'
                    {
                    match(input,23,FOLLOW_23_in_lhs_unary3378); 
                    following.push(FOLLOW_opt_eol_in_lhs_unary3380);
                    opt_eol();
                    following.pop();

                    following.push(FOLLOW_lhs_in_lhs_unary3384);
                    u=lhs();
                    following.pop();

                    following.push(FOLLOW_opt_eol_in_lhs_unary3386);
                    opt_eol();
                    following.pop();

                    match(input,25,FOLLOW_25_in_lhs_unary3388); 
                    d = u;

                    }
                    break;

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_unary


    // $ANTLR start lhs_exist
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1179:1: lhs_exist returns [PatternDescr d] : loc= 'exists' ( '(' column= lhs_column ')' | column= lhs_column ) ;
    public PatternDescr lhs_exist() throws RecognitionException {   
        PatternDescr d;
        Token loc=null;
        PatternDescr column = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1183:17: (loc= 'exists' ( '(' column= lhs_column ')' | column= lhs_column ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1183:17: loc= 'exists' ( '(' column= lhs_column ')' | column= lhs_column )
            {
            loc=(Token)input.LT(1);
            match(input,52,FOLLOW_52_in_lhs_exist3419); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1183:30: ( '(' column= lhs_column ')' | column= lhs_column )
            int alt73=2;
            int LA73_0 = input.LA(1);
            if ( LA73_0==23 ) {
                alt73=1;
            }
            else if ( LA73_0==ID ) {
                alt73=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1183:30: ( \'(\' column= lhs_column \')\' | column= lhs_column )", 73, 0, input);

                throw nvae;
            }
            switch (alt73) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1183:31: '(' column= lhs_column ')'
                    {
                    match(input,23,FOLLOW_23_in_lhs_exist3422); 
                    following.push(FOLLOW_lhs_column_in_lhs_exist3426);
                    column=lhs_column();
                    following.pop();

                    match(input,25,FOLLOW_25_in_lhs_exist3428); 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1183:59: column= lhs_column
                    {
                    following.push(FOLLOW_lhs_column_in_lhs_exist3434);
                    column=lhs_column();
                    following.pop();


                    }
                    break;

            }

             
            			d = new ExistsDescr( (ColumnDescr) column ); 
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_exist


    // $ANTLR start lhs_not
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1190:1: lhs_not returns [NotDescr d] : loc= 'not' ( '(' column= lhs_column ')' | column= lhs_column ) ;
    public NotDescr lhs_not() throws RecognitionException {   
        NotDescr d;
        Token loc=null;
        PatternDescr column = null;


        
        		d = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1194:17: (loc= 'not' ( '(' column= lhs_column ')' | column= lhs_column ) )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1194:17: loc= 'not' ( '(' column= lhs_column ')' | column= lhs_column )
            {
            loc=(Token)input.LT(1);
            match(input,53,FOLLOW_53_in_lhs_not3464); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1194:27: ( '(' column= lhs_column ')' | column= lhs_column )
            int alt74=2;
            int LA74_0 = input.LA(1);
            if ( LA74_0==23 ) {
                alt74=1;
            }
            else if ( LA74_0==ID ) {
                alt74=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1194:27: ( \'(\' column= lhs_column \')\' | column= lhs_column )", 74, 0, input);

                throw nvae;
            }
            switch (alt74) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1194:28: '(' column= lhs_column ')'
                    {
                    match(input,23,FOLLOW_23_in_lhs_not3467); 
                    following.push(FOLLOW_lhs_column_in_lhs_not3471);
                    column=lhs_column();
                    following.pop();

                    match(input,25,FOLLOW_25_in_lhs_not3474); 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1194:57: column= lhs_column
                    {
                    following.push(FOLLOW_lhs_column_in_lhs_not3480);
                    column=lhs_column();
                    following.pop();


                    }
                    break;

            }

            
            			d = new NotDescr( (ColumnDescr) column ); 
            			d.setLocation( offset(loc.getLine()), loc.getCharPositionInLine() );
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_not


    // $ANTLR start lhs_eval
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1201:1: lhs_eval returns [PatternDescr d] : 'eval' loc= '(' c= paren_chunk2 ')' ;
    public PatternDescr lhs_eval() throws RecognitionException {   
        PatternDescr d;
        Token loc=null;
        String c = null;


        
        		d = null;
        		String text = "";
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1206:17: ( 'eval' loc= '(' c= paren_chunk2 ')' )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1206:17: 'eval' loc= '(' c= paren_chunk2 ')'
            {
            match(input,54,FOLLOW_54_in_lhs_eval3506); 
            loc=(Token)input.LT(1);
            match(input,23,FOLLOW_23_in_lhs_eval3510); 
            following.push(FOLLOW_paren_chunk2_in_lhs_eval3518);
            c=paren_chunk2();
            following.pop();

            match(input,25,FOLLOW_25_in_lhs_eval3522); 
             
            			checkTrailingSemicolon( c, offset(loc.getLine()) );
            			d = new EvalDescr( c ); 
            		

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return d;
    }
    // $ANTLR end lhs_eval


    // $ANTLR start dotted_name
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1215:1: dotted_name returns [String name] : id= ID ( '.' id= ID )* ( '[' ']' )* ;
    public String dotted_name() throws RecognitionException {   
        String name;
        Token id=null;

        
        		name = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:17: (id= ID ( '.' id= ID )* ( '[' ']' )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:17: id= ID ( '.' id= ID )* ( '[' ']' )*
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_dotted_name3554); 
             name=id.getText(); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:46: ( '.' id= ID )*
            loop75:
            do {
                int alt75=2;
                int LA75_0 = input.LA(1);
                if ( LA75_0==18 ) {
                    alt75=1;
                }


                switch (alt75) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:48: '.' id= ID
            	    {
            	    match(input,18,FOLLOW_18_in_dotted_name3560); 
            	    id=(Token)input.LT(1);
            	    match(input,ID,FOLLOW_ID_in_dotted_name3564); 
            	     name = name + "." + id.getText(); 

            	    }
            	    break;

            	default :
            	    break loop75;
                }
            } while (true);

            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:99: ( '[' ']' )*
            loop76:
            do {
                int alt76=2;
                int LA76_0 = input.LA(1);
                if ( LA76_0==55 ) {
                    alt76=1;
                }


                switch (alt76) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1220:101: '[' ']'
            	    {
            	    match(input,55,FOLLOW_55_in_dotted_name3573); 
            	    match(input,56,FOLLOW_56_in_dotted_name3575); 
            	     name = name + "[]";

            	    }
            	    break;

            	default :
            	    break loop76;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return name;
    }
    // $ANTLR end dotted_name


    // $ANTLR start argument_name
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1223:1: argument_name returns [String name] : id= ID ( '[' ']' )* ;
    public String argument_name() throws RecognitionException {   
        String name;
        Token id=null;

        
        		name = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1228:17: (id= ID ( '[' ']' )* )
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1228:17: id= ID ( '[' ']' )*
            {
            id=(Token)input.LT(1);
            match(input,ID,FOLLOW_ID_in_argument_name3605); 
             name=id.getText(); 
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1228:46: ( '[' ']' )*
            loop77:
            do {
                int alt77=2;
                int LA77_0 = input.LA(1);
                if ( LA77_0==55 ) {
                    alt77=1;
                }


                switch (alt77) {
            	case 1 :
            	    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1228:48: '[' ']'
            	    {
            	    match(input,55,FOLLOW_55_in_argument_name3611); 
            	    match(input,56,FOLLOW_56_in_argument_name3613); 
            	     name = name + "[]";

            	    }
            	    break;

            	default :
            	    break loop77;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return name;
    }
    // $ANTLR end argument_name


    // $ANTLR start word
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1232:1: word returns [String word] : (id= ID | 'import' | 'use' | 'rule' | 'query' | 'salience' | 'no-loop' | 'when' | 'then' | 'end' | str= STRING );
    public String word() throws RecognitionException {   
        String word;
        Token id=null;
        Token str=null;

        
        		word = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1236:17: (id= ID | 'import' | 'use' | 'rule' | 'query' | 'salience' | 'no-loop' | 'when' | 'then' | 'end' | str= STRING )
            int alt78=11;
            switch ( input.LA(1) ) {
            case ID:
                alt78=1;
                break;
            case 17:
                alt78=2;
                break;
            case 57:
                alt78=3;
                break;
            case 31:
                alt78=4;
                break;
            case 28:
                alt78=5;
                break;
            case 36:
                alt78=6;
                break;
            case 37:
                alt78=7;
                break;
            case 32:
                alt78=8;
                break;
            case 34:
                alt78=9;
                break;
            case 29:
                alt78=10;
                break;
            case STRING:
                alt78=11;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1232:1: word returns [String word] : (id= ID | \'import\' | \'use\' | \'rule\' | \'query\' | \'salience\' | \'no-loop\' | \'when\' | \'then\' | \'end\' | str= STRING );", 78, 0, input);

                throw nvae;
            }

            switch (alt78) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1236:17: id= ID
                    {
                    id=(Token)input.LT(1);
                    match(input,ID,FOLLOW_ID_in_word3641); 
                     word=id.getText(); 

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1237:17: 'import'
                    {
                    match(input,17,FOLLOW_17_in_word3653); 
                     word="import"; 

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1238:17: 'use'
                    {
                    match(input,57,FOLLOW_57_in_word3662); 
                     word="use"; 

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1239:17: 'rule'
                    {
                    match(input,31,FOLLOW_31_in_word3674); 
                     word="rule"; 

                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1240:17: 'query'
                    {
                    match(input,28,FOLLOW_28_in_word3685); 
                     word="query"; 

                    }
                    break;
                case 6 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1241:17: 'salience'
                    {
                    match(input,36,FOLLOW_36_in_word3695); 
                     word="salience"; 

                    }
                    break;
                case 7 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1242:17: 'no-loop'
                    {
                    match(input,37,FOLLOW_37_in_word3703); 
                     word="no-loop"; 

                    }
                    break;
                case 8 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1243:17: 'when'
                    {
                    match(input,32,FOLLOW_32_in_word3711); 
                     word="when"; 

                    }
                    break;
                case 9 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1244:17: 'then'
                    {
                    match(input,34,FOLLOW_34_in_word3722); 
                     word="then"; 

                    }
                    break;
                case 10 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1245:17: 'end'
                    {
                    match(input,29,FOLLOW_29_in_word3733); 
                     word="end"; 

                    }
                    break;
                case 11 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1246:17: str= STRING
                    {
                    str=(Token)input.LT(1);
                    match(input,STRING,FOLLOW_STRING_in_word3747); 
                     word=getString(str);

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return word;
    }
    // $ANTLR end word


    // $ANTLR start operator
    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1249:1: operator returns [String str] : ( '==' | '=' | '>' | '>=' | '<' | '<=' | '!=' | 'contains' | 'matches' | 'excludes' );
    public String operator() throws RecognitionException {   
        String str;
        
        		str = null;
        	
        try {
            // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1255:17: ( '==' | '=' | '>' | '>=' | '<' | '<=' | '!=' | 'contains' | 'matches' | 'excludes' )
            int alt79=10;
            switch ( input.LA(1) ) {
            case 58:
                alt79=1;
                break;
            case 59:
                alt79=2;
                break;
            case 60:
                alt79=3;
                break;
            case 61:
                alt79=4;
                break;
            case 62:
                alt79=5;
                break;
            case 63:
                alt79=6;
                break;
            case 64:
                alt79=7;
                break;
            case 65:
                alt79=8;
                break;
            case 66:
                alt79=9;
                break;
            case 67:
                alt79=10;
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1249:1: operator returns [String str] : ( \'==\' | \'=\' | \'>\' | \'>=\' | \'<\' | \'<=\' | \'!=\' | \'contains\' | \'matches\' | \'excludes\' );", 79, 0, input);

                throw nvae;
            }

            switch (alt79) {
                case 1 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1255:17: '=='
                    {
                    match(input,58,FOLLOW_58_in_operator3776); 
                    str= "==";

                    }
                    break;
                case 2 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1256:18: '='
                    {
                    match(input,59,FOLLOW_59_in_operator3783); 
                    str="==";

                    }
                    break;
                case 3 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1257:18: '>'
                    {
                    match(input,60,FOLLOW_60_in_operator3790); 
                    str=">";

                    }
                    break;
                case 4 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1258:18: '>='
                    {
                    match(input,61,FOLLOW_61_in_operator3797); 
                    str=">=";

                    }
                    break;
                case 5 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1259:18: '<'
                    {
                    match(input,62,FOLLOW_62_in_operator3806); 
                    str="<";

                    }
                    break;
                case 6 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1260:18: '<='
                    {
                    match(input,63,FOLLOW_63_in_operator3813); 
                    str="<=";

                    }
                    break;
                case 7 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1261:18: '!='
                    {
                    match(input,64,FOLLOW_64_in_operator3820); 
                    str="!=";

                    }
                    break;
                case 8 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1262:18: 'contains'
                    {
                    match(input,65,FOLLOW_65_in_operator3827); 
                    str="contains";

                    }
                    break;
                case 9 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1263:18: 'matches'
                    {
                    match(input,66,FOLLOW_66_in_operator3834); 
                    str="matches";

                    }
                    break;
                case 10 :
                    // D:\dev\drools-3.1\drools-compiler\src\main\resources\org\drools\lang\drl2.g:1264:18: 'excludes'
                    {
                    match(input,67,FOLLOW_67_in_operator3841); 
                    str="excludes";

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return str;
    }
    // $ANTLR end operator


    protected DFA2 dfa2 = new DFA2();protected DFA4 dfa4 = new DFA4();protected DFA12 dfa12 = new DFA12();protected DFA13 dfa13 = new DFA13();protected DFA14 dfa14 = new DFA14();protected DFA46 dfa46 = new DFA46();protected DFA47 dfa47 = new DFA47();protected DFA48 dfa48 = new DFA48();protected DFA56 dfa56 = new DFA56();protected DFA59 dfa59 = new DFA59();protected DFA60 dfa60 = new DFA60();
    class DFA2 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s1 = new DFA.State() {{alt=5;}};
        DFA.State s5 = new DFA.State() {{alt=3;}};
        DFA.State s4 = new DFA.State() {{alt=2;}};
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s2 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 30:
                    return s5;

                case EOL:
                case 15:
                    return s2;

                case 28:
                    return s4;

                case 31:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 2, input);

                    throw nvae;        }
            }
        };
        DFA.State s6 = new DFA.State() {{alt=4;}};
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case -1:
                    return s1;

                case EOL:
                case 15:
                    return s2;

                case 31:
                    return s3;

                case 28:
                    return s4;

                case 30:
                    return s5;

                case 17:
                case 21:
                case 22:
                    return s6;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 0, input);

                    throw nvae;        }
            }
        };

    }class DFA4 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s1 = new DFA.State() {{alt=3;}};
        DFA.State s11 = new DFA.State() {{alt=1;}};
        DFA.State s10 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_10 = input.LA(1);
                if ( LA4_10==ID ) {return s11;}
                if ( LA4_10==EOL||LA4_10==15 ) {return s10;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 10, input);

                throw nvae;
            }
        };
        DFA.State s5 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_5 = input.LA(1);
                if ( LA4_5==EOL||LA4_5==15 ) {return s10;}
                if ( LA4_5==ID ) {return s11;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 5, input);

                throw nvae;
            }
        };
        DFA.State s19 = new DFA.State() {{alt=1;}};
        DFA.State s28 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_28 = input.LA(1);
                if ( LA4_28==ID ) {return s19;}
                if ( LA4_28==55 ) {return s18;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 28, input);

                throw nvae;
            }
        };
        DFA.State s18 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_18 = input.LA(1);
                if ( LA4_18==56 ) {return s28;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 18, input);

                throw nvae;
            }
        };
        DFA.State s27 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 55:
                    return s18;

                case ID:
                    return s19;

                case 18:
                    return s17;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 27, input);

                    throw nvae;        }
            }
        };
        DFA.State s17 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_17 = input.LA(1);
                if ( LA4_17==ID ) {return s27;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 17, input);

                throw nvae;
            }
        };
        DFA.State s12 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 18:
                    return s17;

                case 55:
                    return s18;

                case ID:
                    return s19;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 12, input);

                    throw nvae;        }
            }
        };
        DFA.State s6 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_6 = input.LA(1);
                if ( LA4_6==ID ) {return s12;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 6, input);

                throw nvae;
            }
        };
        DFA.State s115 = new DFA.State() {{alt=1;}};
        DFA.State s121 = new DFA.State() {{alt=1;}};
        DFA.State s123 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s121;

                case 26:
                    return s122;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s123;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 123, input);

                    throw nvae;        }
            }
        };
        DFA.State s122 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s121;

                case 26:
                    return s122;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s123;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 122, input);

                    throw nvae;        }
            }
        };
        DFA.State s116 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s121;

                case 26:
                    return s122;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s123;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 116, input);

                    throw nvae;        }
            }
        };
        DFA.State s117 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s115;

                case 26:
                    return s116;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s117;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 117, input);

                    throw nvae;        }
            }
        };
        DFA.State s106 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s115;

                case 26:
                    return s116;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s117;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 106, input);

                    throw nvae;        }
            }
        };
        DFA.State s107 = new DFA.State() {{alt=1;}};
        DFA.State s108 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s107;

                case 26:
                    return s106;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s108;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 108, input);

                    throw nvae;        }
            }
        };
        DFA.State s88 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 26:
                    return s106;

                case 27:
                    return s107;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s108;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 88, input);

                    throw nvae;        }
            }
        };
        DFA.State s89 = new DFA.State() {{alt=1;}};
        DFA.State s90 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s89;

                case 26:
                    return s88;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s90;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 90, input);

                    throw nvae;        }
            }
        };
        DFA.State s68 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 26:
                    return s88;

                case 27:
                    return s89;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s90;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 68, input);

                    throw nvae;        }
            }
        };
        DFA.State s69 = new DFA.State() {{alt=1;}};
        DFA.State s70 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 27:
                    return s69;

                case 26:
                    return s68;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s70;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 70, input);

                    throw nvae;        }
            }
        };
        DFA.State s54 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 26:
                    return s68;

                case 27:
                    return s69;

                case EOL:
                case ID:
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case MISC:
                case WS:
                case SH_STYLE_SINGLE_LINE_COMMENT:
                case C_STYLE_SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                case 33:
                case 34:
                case 35:
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 43:
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s70;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 54, input);

                    throw nvae;        }
            }
        };
        DFA.State s53 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_53 = input.LA(1);
                if ( LA4_53==26 ) {return s54;}
                if ( LA4_53==EOL||LA4_53==15 ) {return s53;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 53, input);

                throw nvae;
            }
        };
        DFA.State s36 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_36 = input.LA(1);
                if ( LA4_36==EOL||LA4_36==15 ) {return s53;}
                if ( LA4_36==26 ) {return s54;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 36, input);

                throw nvae;
            }
        };
        DFA.State s103 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 25:
                    return s36;

                case 24:
                    return s51;

                case EOL:
                case 15:
                    return s103;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 103, input);

                    throw nvae;        }
            }
        };
        DFA.State s87 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 55:
                    return s60;

                case EOL:
                case 15:
                    return s103;

                case 25:
                    return s36;

                case 24:
                    return s51;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 87, input);

                    throw nvae;        }
            }
        };
        DFA.State s83 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 25:
                    return s36;

                case 24:
                    return s51;

                case EOL:
                case 15:
                    return s83;

                case ID:
                    return s87;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 83, input);

                    throw nvae;        }
            }
        };
        DFA.State s65 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 55:
                    return s48;

                case EOL:
                case 15:
                    return s83;

                case 25:
                    return s36;

                case 24:
                    return s51;

                case 18:
                    return s17;

                case ID:
                    return s87;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 65, input);

                    throw nvae;        }
            }
        };
        DFA.State s64 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_64 = input.LA(1);
                if ( LA4_64==ID ) {return s65;}
                if ( LA4_64==EOL||LA4_64==15 ) {return s64;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 64, input);

                throw nvae;
            }
        };
        DFA.State s51 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_51 = input.LA(1);
                if ( LA4_51==EOL||LA4_51==15 ) {return s64;}
                if ( LA4_51==ID ) {return s65;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 51, input);

                throw nvae;
            }
        };
        DFA.State s61 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 24:
                    return s51;

                case 25:
                    return s36;

                case EOL:
                case 15:
                    return s61;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 61, input);

                    throw nvae;        }
            }
        };
        DFA.State s76 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s61;

                case 24:
                    return s51;

                case 25:
                    return s36;

                case 55:
                    return s60;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 76, input);

                    throw nvae;        }
            }
        };
        DFA.State s60 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_60 = input.LA(1);
                if ( LA4_60==56 ) {return s76;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 60, input);

                throw nvae;
            }
        };
        DFA.State s50 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 55:
                    return s60;

                case EOL:
                case 15:
                    return s61;

                case 24:
                    return s51;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 50, input);

                    throw nvae;        }
            }
        };
        DFA.State s49 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                    return s50;

                case EOL:
                case 15:
                    return s49;

                case 24:
                    return s51;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 49, input);

                    throw nvae;        }
            }
        };
        DFA.State s55 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s49;

                case ID:
                    return s50;

                case 55:
                    return s48;

                case 24:
                    return s51;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 55, input);

                    throw nvae;        }
            }
        };
        DFA.State s48 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_48 = input.LA(1);
                if ( LA4_48==56 ) {return s55;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 48, input);

                throw nvae;
            }
        };
        DFA.State s35 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 18:
                    return s17;

                case 55:
                    return s48;

                case EOL:
                case 15:
                    return s49;

                case ID:
                    return s50;

                case 24:
                    return s51;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 35, input);

                    throw nvae;        }
            }
        };
        DFA.State s34 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                    return s35;

                case EOL:
                case 15:
                    return s34;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 34, input);

                    throw nvae;        }
            }
        };
        DFA.State s26 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s34;

                case ID:
                    return s35;

                case 25:
                    return s36;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 26, input);

                    throw nvae;        }
            }
        };
        DFA.State s32 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_32 = input.LA(1);
                if ( LA4_32==23 ) {return s26;}
                if ( LA4_32==EOL||LA4_32==15 ) {return s32;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 32, input);

                throw nvae;
            }
        };
        DFA.State s25 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_25 = input.LA(1);
                if ( LA4_25==EOL||LA4_25==15 ) {return s32;}
                if ( LA4_25==23 ) {return s26;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 25, input);

                throw nvae;
            }
        };
        DFA.State s24 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                    return s25;

                case EOL:
                case 15:
                    return s24;

                case 23:
                    return s26;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 24, input);

                    throw nvae;        }
            }
        };
        DFA.State s14 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 18:
                    return s17;

                case 55:
                    return s18;

                case EOL:
                case 15:
                    return s24;

                case ID:
                    return s25;

                case 23:
                    return s26;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 14, input);

                    throw nvae;        }
            }
        };
        DFA.State s13 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_13 = input.LA(1);
                if ( LA4_13==ID ) {return s14;}
                if ( LA4_13==EOL||LA4_13==15 ) {return s13;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 13, input);

                throw nvae;
            }
        };
        DFA.State s7 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA4_7 = input.LA(1);
                if ( LA4_7==EOL||LA4_7==15 ) {return s13;}
                if ( LA4_7==ID ) {return s14;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 4, 7, input);

                throw nvae;
            }
        };
        DFA.State s9 = new DFA.State() {{alt=2;}};
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case -1:
                case EOL:
                case 15:
                case 28:
                case 30:
                case 31:
                    return s1;

                case 17:
                    return s5;

                case 21:
                    return s6;

                case 22:
                    return s7;

                case 20:
                    return s9;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 0, input);

                    throw nvae;        }
            }
        };

    }class DFA12 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s2 = new DFA.State() {{alt=2;}};
        DFA.State s5 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                    return s3;

                case EOL:
                case 15:
                    return s5;

                case 23:
                    return s2;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 12, 5, input);

                    throw nvae;        }
            }
        };
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                case 18:
                case 55:
                    return s3;

                case EOL:
                case 15:
                    return s5;

                case 23:
                    return s2;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 12, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA12_0 = input.LA(1);
                if ( LA12_0==ID ) {return s1;}
                if ( LA12_0==EOL||LA12_0==15 ) {return s2;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 12, 0, input);

                throw nvae;
            }
        };

    }class DFA13 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s2 = new DFA.State() {{alt=2;}};
        DFA.State s7 = new DFA.State() {{alt=1;}};
        DFA.State s4 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 24:
                case 25:
                    return s2;

                case EOL:
                case 15:
                    return s4;

                case ID:
                    return s7;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 13, 4, input);

                    throw nvae;        }
            }
        };
        DFA.State s9 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s4;

                case ID:
                    return s7;

                case 55:
                    return s3;

                case 24:
                case 25:
                    return s2;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 13, 9, input);

                    throw nvae;        }
            }
        };
        DFA.State s3 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA13_3 = input.LA(1);
                if ( LA13_3==56 ) {return s9;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 13, 3, input);

                throw nvae;
            }
        };
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 55:
                    return s3;

                case EOL:
                case 15:
                    return s4;

                case 24:
                case 25:
                    return s2;

                case ID:
                case 18:
                    return s7;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 13, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA13_0 = input.LA(1);
                if ( LA13_0==ID ) {return s1;}
                if ( LA13_0==EOL||LA13_0==15 ) {return s2;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }
        };

    }class DFA14 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s2 = new DFA.State() {{alt=2;}};
        DFA.State s5 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 24:
                case 25:
                    return s2;

                case EOL:
                case 15:
                    return s5;

                case ID:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 14, 5, input);

                    throw nvae;        }
            }
        };
        DFA.State s9 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s5;

                case 24:
                case 25:
                    return s2;

                case 55:
                    return s4;

                case ID:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 14, 9, input);

                    throw nvae;        }
            }
        };
        DFA.State s4 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA14_4 = input.LA(1);
                if ( LA14_4==56 ) {return s9;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 14, 4, input);

                throw nvae;
            }
        };
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                case 18:
                    return s3;

                case 55:
                    return s4;

                case EOL:
                case 15:
                    return s5;

                case 24:
                case 25:
                    return s2;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 14, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA14_0 = input.LA(1);
                if ( LA14_0==ID ) {return s1;}
                if ( LA14_0==EOL||LA14_0==15 ) {return s2;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 14, 0, input);

                throw nvae;
            }
        };

    }class DFA46 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s4 = new DFA.State() {{alt=2;}};
        DFA.State s2 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 33:
                    return s3;

                case EOL:
                case 15:
                    return s2;

                case 23:
                    return s4;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 46, 2, input);

                    throw nvae;        }
            }
        };
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s2;

                case 33:
                    return s3;

                case 18:
                case 23:
                case 55:
                    return s4;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 46, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA46_0 = input.LA(1);
                if ( LA46_0==ID ) {return s1;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 46, 0, input);

                throw nvae;
            }
        };

    }class DFA47 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s8 = new DFA.State() {{alt=1;}};
        DFA.State s27 = new DFA.State() {{alt=2;}};
        DFA.State s52 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 23:
                case 33:
                    return s8;

                case EOL:
                case 15:
                    return s52;

                case 24:
                case 25:
                    return s27;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 52, input);

                    throw nvae;        }
            }
        };
        DFA.State s31 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s52;

                case 18:
                case 23:
                case 33:
                case 55:
                    return s8;

                case 24:
                case 25:
                    return s27;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 31, input);

                    throw nvae;        }
            }
        };
        DFA.State s26 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case 25:
                case 26:
                case 43:
                    return s27;

                case EOL:
                case 15:
                    return s26;

                case 23:
                case 52:
                case 53:
                case 54:
                    return s8;

                case ID:
                    return s31;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 26, input);

                    throw nvae;        }
            }
        };
        DFA.State s7 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s26;

                case INT:
                case BOOL:
                case STRING:
                case FLOAT:
                case 25:
                case 26:
                case 43:
                    return s27;

                case ID:
                    return s31;

                case 23:
                case 52:
                case 53:
                case 54:
                    return s8;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 7, input);

                    throw nvae;        }
            }
        };
        DFA.State s6 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                case 25:
                case 29:
                case 34:
                case 52:
                case 53:
                case 54:
                    return s8;

                case EOL:
                case 15:
                    return s6;

                case 23:
                    return s7;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 6, input);

                    throw nvae;        }
            }
        };
        DFA.State s5 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s6;

                case 23:
                    return s7;

                case ID:
                case 25:
                case 29:
                case 34:
                case 45:
                case 46:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                    return s8;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 47, 5, input);

                    throw nvae;        }
            }
        };
        DFA.State s2 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA47_2 = input.LA(1);
                if ( LA47_2==ID ) {return s5;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 47, 2, input);

                throw nvae;
            }
        };
        DFA.State s3 = new DFA.State() {{alt=3;}};
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA47_1 = input.LA(1);
                if ( LA47_1==18 ) {return s2;}
                if ( LA47_1==EOL||LA47_1==15||LA47_1==23 ) {return s3;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 47, 1, input);

                throw nvae;
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA47_0 = input.LA(1);
                if ( LA47_0==ID ) {return s1;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 47, 0, input);

                throw nvae;
            }
        };

    }class DFA48 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s2 = new DFA.State() {{alt=2;}};
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 25:
                    return s2;

                case EOL:
                case 15:
                    return s1;

                case 24:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 48, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s1;

                case 25:
                    return s2;

                case 24:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 48, 0, input);

                    throw nvae;        }
            }
        };

    }class DFA56 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s2 = new DFA.State() {{alt=1;}};
        DFA.State s3 = new DFA.State() {{alt=2;}};
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case ID:
                    return s2;

                case EOL:
                case 15:
                    return s1;

                case 25:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 56, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s1;

                case ID:
                    return s2;

                case 25:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 56, 0, input);

                    throw nvae;        }
            }
        };

    }class DFA59 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s3 = new DFA.State() {{alt=1;}};
        DFA.State s2 = new DFA.State() {{alt=2;}};
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case 24:
                    return s3;

                case EOL:
                case 15:
                    return s1;

                case 25:
                    return s2;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 59, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s1;

                case 25:
                    return s2;

                case 24:
                    return s3;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 59, 0, input);

                    throw nvae;        }
            }
        };

    }class DFA60 extends DFA {
        public int predict(IntStream input) throws RecognitionException {
            return predict(input, s0);
        }
        DFA.State s3 = new DFA.State() {{alt=2;}};
        DFA.State s15 = new DFA.State() {{alt=1;}};
        DFA.State s2 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s2;

                case 24:
                case 25:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s3;

                case 33:
                    return s15;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 60, 2, input);

                    throw nvae;        }
            }
        };
        DFA.State s1 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                switch ( input.LA(1) ) {
                case EOL:
                case 15:
                    return s2;

                case 24:
                case 25:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 65:
                case 66:
                case 67:
                    return s3;

                case 33:
                    return s15;

                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 60, 1, input);

                    throw nvae;        }
            }
        };
        DFA.State s0 = new DFA.State() {
            public DFA.State transition(IntStream input) throws RecognitionException {
                int LA60_0 = input.LA(1);
                if ( LA60_0==ID ) {return s1;}

                NoViableAltException nvae =
        	    new NoViableAltException("", 60, 0, input);

                throw nvae;
            }
        };

    }


    public static final BitSet FOLLOW_set_in_opt_eol41 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_compilation_unit57 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_prolog_in_compilation_unit61 = new BitSet(new long[]{0x0000000000628012L});
    public static final BitSet FOLLOW_rule_in_compilation_unit70 = new BitSet(new long[]{0x0000000000628012L});
    public static final BitSet FOLLOW_query_in_compilation_unit83 = new BitSet(new long[]{0x0000000000628012L});
    public static final BitSet FOLLOW_template_in_compilation_unit93 = new BitSet(new long[]{0x0000000000628012L});
    public static final BitSet FOLLOW_extra_statement_in_compilation_unit101 = new BitSet(new long[]{0x0000000000628012L});
    public static final BitSet FOLLOW_opt_eol_in_prolog125 = new BitSet(new long[]{0x0000000000738012L});
    public static final BitSet FOLLOW_package_statement_in_prolog133 = new BitSet(new long[]{0x0000000000728012L});
    public static final BitSet FOLLOW_extra_statement_in_prolog148 = new BitSet(new long[]{0x0000000000728012L});
    public static final BitSet FOLLOW_expander_in_prolog154 = new BitSet(new long[]{0x0000000000728012L});
    public static final BitSet FOLLOW_opt_eol_in_prolog166 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_package_statement190 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_package_statement192 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_dotted_name_in_package_statement196 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_package_statement198 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_package_statement201 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_import_statement217 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_import_statement219 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_import_name_in_import_statement223 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_import_statement225 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_import_statement228 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_import_name259 = new BitSet(new long[]{0x00000000000C0002L});
    public static final BitSet FOLLOW_18_in_import_name265 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_import_name269 = new BitSet(new long[]{0x00000000000C0002L});
    public static final BitSet FOLLOW_19_in_import_name279 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_20_in_expander299 = new BitSet(new long[]{0x0000000000008032L});
    public static final BitSet FOLLOW_dotted_name_in_expander304 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_expander308 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_expander311 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_global335 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_dotted_name_in_global339 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_global343 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_global345 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_global348 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_function372 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function374 = new BitSet(new long[]{0x0000000000008032L});
    public static final BitSet FOLLOW_dotted_name_in_function379 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function383 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_function387 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function389 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_function398 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function400 = new BitSet(new long[]{0x0000000002008032L});
    public static final BitSet FOLLOW_dotted_name_in_function410 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function414 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_argument_name_in_function418 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function420 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_function434 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function436 = new BitSet(new long[]{0x0000000000008032L});
    public static final BitSet FOLLOW_dotted_name_in_function441 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function445 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_argument_name_in_function449 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function451 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_function476 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function480 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_26_in_function484 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_curly_chunk_in_function491 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_27_in_function500 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_function508 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_opt_eol_in_query532 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_28_in_query538 = new BitSet(new long[]{0x02000035B0020120L});
    public static final BitSet FOLLOW_word_in_query542 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_query544 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_expander_lhs_block_in_query560 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_normal_lhs_block_in_query568 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_29_in_query583 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_query585 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_opt_eol_in_template609 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_30_in_template615 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_template619 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EOL_in_template621 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_template_slot_in_template636 = new BitSet(new long[]{0x0000000020000020L});
    public static final BitSet FOLLOW_29_in_template651 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EOL_in_template653 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_dotted_name_in_template_slot685 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_template_slot689 = new BitSet(new long[]{0x0000000000008010L});
    public static final BitSet FOLLOW_set_in_template_slot693 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_opt_eol_in_rule728 = new BitSet(new long[]{0x0000000080000000L});
    public static final BitSet FOLLOW_31_in_rule734 = new BitSet(new long[]{0x02000035B0020120L});
    public static final BitSet FOLLOW_word_in_rule738 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule740 = new BitSet(new long[]{0x0000000A00008012L});
    public static final BitSet FOLLOW_rule_attributes_in_rule751 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule761 = new BitSet(new long[]{0x0000000120008012L});
    public static final BitSet FOLLOW_32_in_rule770 = new BitSet(new long[]{0x0000000200008012L});
    public static final BitSet FOLLOW_33_in_rule772 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule775 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_expander_lhs_block_in_rule793 = new BitSet(new long[]{0x0000000020008012L});
    public static final BitSet FOLLOW_normal_lhs_block_in_rule802 = new BitSet(new long[]{0x0000000020008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule825 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_34_in_rule829 = new BitSet(new long[]{0x0000000200008012L});
    public static final BitSet FOLLOW_33_in_rule831 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule835 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF0L,0x000000000000000FL});
    public static final BitSet FOLLOW_29_in_rule881 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule883 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_import_statement_in_extra_statement903 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_global_in_extra_statement908 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_function_in_extra_statement913 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_rule_attributes932 = new BitSet(new long[]{0x0000000200008012L});
    public static final BitSet FOLLOW_33_in_rule_attributes935 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule_attributes938 = new BitSet(new long[]{0x000003F001000002L});
    public static final BitSet FOLLOW_24_in_rule_attributes945 = new BitSet(new long[]{0x000003F000000000L});
    public static final BitSet FOLLOW_rule_attribute_in_rule_attributes950 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_rule_attributes952 = new BitSet(new long[]{0x000003F001000002L});
    public static final BitSet FOLLOW_salience_in_rule_attribute991 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_no_loop_in_rule_attribute1001 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_agenda_group_in_rule_attribute1012 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_duration_in_rule_attribute1025 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_activation_group_in_rule_attribute1039 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_auto_focus_in_rule_attribute1050 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_salience1083 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_salience1085 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_INT_in_salience1089 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_salience1091 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_salience1094 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_no_loop1129 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_no_loop1131 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_no_loop1133 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_no_loop1136 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_no_loop1161 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_BOOL_in_no_loop1165 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_no_loop1167 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_no_loop1169 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_no_loop1172 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_auto_focus1218 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_auto_focus1220 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_auto_focus1222 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_auto_focus1225 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_auto_focus1250 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_BOOL_in_auto_focus1254 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_auto_focus1256 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_auto_focus1258 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_auto_focus1261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_activation_group1303 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_activation_group1305 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_STRING_in_activation_group1309 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_activation_group1311 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_activation_group1314 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_agenda_group1343 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_agenda_group1345 = new BitSet(new long[]{0x0000000000000100L});
    public static final BitSet FOLLOW_STRING_in_agenda_group1349 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_agenda_group1351 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_agenda_group1354 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_duration1386 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_duration1388 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_INT_in_duration1392 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_15_in_duration1394 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_duration1397 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_in_normal_lhs_block1423 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_normal_lhs_block1425 = new BitSet(new long[]{0x0070000000808032L});
    public static final BitSet FOLLOW_opt_eol_in_normal_lhs_block1437 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_paren_chunk_in_expander_lhs_block1478 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_EOL_in_expander_lhs_block1482 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_EOL_in_expander_lhs_block1497 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_lhs_or_in_lhs1539 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fact_binding_in_lhs_column1567 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fact_in_lhs_column1576 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_from_statement1604 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_statement1606 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_from_source_in_from_statement1610 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_from_source1654 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_18_in_from_source1656 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_from_source1660 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_from_source1687 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_18_in_from_source1689 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_from_source1693 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1695 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_from_source1698 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1700 = new BitSet(new long[]{0x00000800040003E2L});
    public static final BitSet FOLLOW_argument_list_in_from_source1704 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1706 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_from_source1708 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_from_source1730 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1732 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_from_source1734 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1736 = new BitSet(new long[]{0x00000800040003E2L});
    public static final BitSet FOLLOW_argument_list_in_from_source1740 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_from_source1742 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_from_source1744 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_argument_value_in_argument_list1787 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_argument_list1803 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_argument_list1805 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_argument_list1807 = new BitSet(new long[]{0x00000800040003E0L});
    public static final BitSet FOLLOW_argument_value_in_argument_list1811 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_STRING_in_argument_value1851 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_argument_value1862 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FLOAT_in_argument_value1875 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOL_in_argument_value1886 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_argument_value1898 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_argument_value1909 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_argument_value1920 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_inline_map_in_argument_value1939 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_inline_map1979 = new BitSet(new long[]{0x00000800040003E0L});
    public static final BitSet FOLLOW_argument_value_in_inline_map1997 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_44_in_inline_map1999 = new BitSet(new long[]{0x00000800040003E0L});
    public static final BitSet FOLLOW_argument_value_in_inline_map2003 = new BitSet(new long[]{0x0000000009000010L});
    public static final BitSet FOLLOW_EOL_in_inline_map2046 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_inline_map2050 = new BitSet(new long[]{0x00000800040003F0L});
    public static final BitSet FOLLOW_EOL_in_inline_map2053 = new BitSet(new long[]{0x00000800040003E0L});
    public static final BitSet FOLLOW_argument_value_in_inline_map2059 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_44_in_inline_map2061 = new BitSet(new long[]{0x00000800040003E0L});
    public static final BitSet FOLLOW_argument_value_in_inline_map2065 = new BitSet(new long[]{0x0000000009000010L});
    public static final BitSet FOLLOW_27_in_inline_map2101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_fact_binding2133 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_binding2143 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_33_in_fact_binding2145 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_binding2147 = new BitSet(new long[]{0x0000000000800020L});
    public static final BitSet FOLLOW_fact_expression_in_fact_binding2151 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_fact_expression2183 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_expression2185 = new BitSet(new long[]{0x0000000000800020L});
    public static final BitSet FOLLOW_fact_expression_in_fact_expression2189 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_expression2191 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_fact_expression2193 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_fact_in_fact_expression2204 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_expression2206 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_set_in_fact_expression2219 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact_expression2224 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_fact_in_fact_expression2238 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_dotted_name_in_fact2277 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact2285 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_fact2293 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact2296 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_constraints_in_fact2302 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact2321 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_fact2325 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_fact2327 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_opt_eol_in_constraints2359 = new BitSet(new long[]{0x0000000000008032L});
    public static final BitSet FOLLOW_constraint_in_constraints2364 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_predicate_in_constraints2367 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraints2375 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_constraints2377 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraints2379 = new BitSet(new long[]{0x0000000000008032L});
    public static final BitSet FOLLOW_constraint_in_constraints2382 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_predicate_in_constraints2385 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraints2393 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2412 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_constraint2420 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2422 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_33_in_constraint2424 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2426 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_constraint2436 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2450 = new BitSet(new long[]{0xFC00000000008012L,0x000000000000000FL});
    public static final BitSet FOLLOW_operator_in_constraint2456 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2458 = new BitSet(new long[]{0x00000800008003E0L});
    public static final BitSet FOLLOW_ID_in_constraint2476 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_enum_constraint_in_constraint2501 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_literal_constraint_in_constraint2533 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_retval_constraint_in_constraint2553 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_set_in_constraint2588 = new BitSet(new long[]{0xFC00000000000000L,0x000000000000000FL});
    public static final BitSet FOLLOW_operator_in_constraint2610 = new BitSet(new long[]{0x00000800008003E0L});
    public static final BitSet FOLLOW_ID_in_constraint2622 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_enum_constraint_in_constraint2650 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_literal_constraint_in_constraint2685 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_retval_constraint_in_constraint2707 = new BitSet(new long[]{0x0001800000008012L});
    public static final BitSet FOLLOW_opt_eol_in_constraint2763 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_literal_constraint2790 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_literal_constraint2801 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FLOAT_in_literal_constraint2814 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOL_in_literal_constraint2825 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_literal_constraint2837 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_enum_constraint2868 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_18_in_enum_constraint2870 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_enum_constraint2874 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_retval_constraint2903 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_paren_chunk_in_retval_constraint2908 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_retval_constraint2911 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_predicate2929 = new BitSet(new long[]{0x0000000200000000L});
    public static final BitSet FOLLOW_33_in_predicate2931 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_predicate2935 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_49_in_predicate2937 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_predicate2939 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_paren_chunk_in_predicate2943 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_predicate2945 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_paren_chunk2991 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_paren_chunk_in_paren_chunk2995 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_paren_chunk2997 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_23_in_paren_chunk23068 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_paren_chunk2_in_paren_chunk23072 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_paren_chunk23074 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_26_in_curly_chunk3143 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_curly_chunk_in_curly_chunk3147 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_27_in_curly_chunk3149 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_lhs_and_in_lhs_or3207 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_set_in_lhs_or3216 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_lhs_or3221 = new BitSet(new long[]{0x0070000000800020L});
    public static final BitSet FOLLOW_lhs_and_in_lhs_or3228 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_lhs_unary_in_lhs_and3268 = new BitSet(new long[]{0x000C000000000002L});
    public static final BitSet FOLLOW_set_in_lhs_and3277 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_lhs_and3282 = new BitSet(new long[]{0x0070000000800020L});
    public static final BitSet FOLLOW_lhs_unary_in_lhs_and3289 = new BitSet(new long[]{0x000C000000000002L});
    public static final BitSet FOLLOW_lhs_exist_in_lhs_unary3327 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_not_in_lhs_unary3337 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_eval_in_lhs_unary3347 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_column_in_lhs_unary3361 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_from_statement_in_lhs_unary3368 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_lhs_unary3378 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_lhs_unary3380 = new BitSet(new long[]{0x0070000000800020L});
    public static final BitSet FOLLOW_lhs_in_lhs_unary3384 = new BitSet(new long[]{0x0000000000008012L});
    public static final BitSet FOLLOW_opt_eol_in_lhs_unary3386 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_lhs_unary3388 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_52_in_lhs_exist3419 = new BitSet(new long[]{0x0000000000800020L});
    public static final BitSet FOLLOW_23_in_lhs_exist3422 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_lhs_column_in_lhs_exist3426 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_lhs_exist3428 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_column_in_lhs_exist3434 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_lhs_not3464 = new BitSet(new long[]{0x0000000000800020L});
    public static final BitSet FOLLOW_23_in_lhs_not3467 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_lhs_column_in_lhs_not3471 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_lhs_not3474 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lhs_column_in_lhs_not3480 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_54_in_lhs_eval3506 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_lhs_eval3510 = new BitSet(new long[]{0xFFFFFFFFFFFFFFF2L,0x000000000000000FL});
    public static final BitSet FOLLOW_paren_chunk2_in_lhs_eval3518 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_lhs_eval3522 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_dotted_name3554 = new BitSet(new long[]{0x0080000000040002L});
    public static final BitSet FOLLOW_18_in_dotted_name3560 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_ID_in_dotted_name3564 = new BitSet(new long[]{0x0080000000040002L});
    public static final BitSet FOLLOW_55_in_dotted_name3573 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_56_in_dotted_name3575 = new BitSet(new long[]{0x0080000000000002L});
    public static final BitSet FOLLOW_ID_in_argument_name3605 = new BitSet(new long[]{0x0080000000000002L});
    public static final BitSet FOLLOW_55_in_argument_name3611 = new BitSet(new long[]{0x0100000000000000L});
    public static final BitSet FOLLOW_56_in_argument_name3613 = new BitSet(new long[]{0x0080000000000002L});
    public static final BitSet FOLLOW_ID_in_word3641 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_word3653 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_57_in_word3662 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_word3674 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_word3685 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_word3695 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_word3703 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_word3711 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_word3722 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_word3733 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_word3747 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_58_in_operator3776 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_59_in_operator3783 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_60_in_operator3790 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_operator3797 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_operator3806 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_63_in_operator3813 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_64_in_operator3820 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_65_in_operator3827 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_66_in_operator3834 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_67_in_operator3841 = new BitSet(new long[]{0x0000000000000002L});

}