#! /usr/bin/perl -w

=head1 NAME

Convert - Converter for HTML documents exported from Microsoft Word 

=head1 SYNOPSIS

 Convert.pl [OPTIONS] input.html [>output.html]

 OPTIONS

 -h             prints the usage message
 -i <prefix>    adds unique prefix to image names.
 -c <format>    cuts into chapters. Format is used to name the resulting files
 -t <toc_file>  writes table of contents
 -S <CSS file>  forces using of specified stylesheet file
 -u             leaves links unprocessed, don't add Javascript mouseover/mousout
 -n <number>    when using -c, make length of all chapter file names less then <number>.
                Default is 8.
 -l <number>    when using -c, specifies from what heading level new chapter file
                will be created. Default is 1.
 -p             when using -c, preservers "ChapterN" or "AppendixN" word in the 
                chapter file name
 -a             when using -c, does not add the JS global variable "file_name" filled with
 		file name of chapter. By default variable is added.
 -P <name>	product name. Used in the templates.
 -X <plugin-name> Eclipse plugin title. If not defined, the value of -P will be used.

E.g.:

 perl -w ..\Convert.pl -i images\clt_ -c EMLClt_%s -t EMLClt_TOC.html -P "Exigen Everything" -x "Exigen Guide To Everything" "User Guide.htm"


=head1 Description

If no option is given, most of the style etc. directives are deleted from the
file, empty paragraphs are deleted, history of document changes (C<< <del> >>
and C<< <ins> >>) are hidden.  Entities with specific values of the C<class>
attribute are treated in an appropriate way, e.g., lists are presented as
ordered or unordered lists. If there is an only F<*.css> file in the working
directory it is used. Resulting file goes to standard output. If there are
more than one F<*.css> file in the working directory then no stylesheet
files will be used due to unability to make proper decision. If the
stylesheet file is specified in command line then only it will be used 
without any further searching.

Option C<-i> allows us to copy images used in the file, giving them a
distinctive prefix. This prefix could be directory name that will be
automatically created. 

C<-c> says to cut the document into chapters (C<< <h1> >>). The file names
are constructed according to the given format - C<%d> for the chapter number,
C<%s>
for the condensed, StudlyCapped title of the chapter. The usual C<printf>
flags can be inserted between the percent sign and the letter, e.g., C<%04d>
will
left-pad the chapter number with zeros to 4 digits ("0002"). Names of the
files being created will be printed out.

C<-t> will create the table of contents in the specified file.


=head1 Getting

Perl can be downloaded from, e.g., ActiveState's site (L<http://www.activestate.com>). 

You will probably need a newer version of the C<HTML-Tree> package than that
bundled with the ActiveState installation. You
can use C<PPM> command to get it. C<installer.bat> may simplify the
procedure.

=over 

=item 1

First set the variables to work with our firewall

 set HTTP_proxy=http://proxy:3128
 set HTTP_proxy_user=<web-name>
 set HTTP_proxy_pass=<web-pass>

=item 2

You may need this

 rem ppm set repository --remove "Active State" 
 ppm set repository AS http://www.ActiveState.com/PPMPackages/5.6

=item 3

Get the library

 ppm install HTML-Tree
 ppm install HTML-Parser

=item 4

You are ready now.

=back

=cut



use HTML::TreeBuilder;
use HTML::Element;
use File::Copy;
use File::Basename;
use IO::Handle;

use integer;


### sorting out the arguments:

my $command_line = " $0 @ARGV ";

my %opt;

do {
  use Getopt::Std;

  getopts('hpuc:i:t:n:S:l:aBP:X:',\%opt);

  use Pod::Usage;

  if ( $opt{h} or @ARGV < 1 ) { pod2usage() }

  $img_prefix = $opt{i} || '';
  $toc_file = $opt{t};
  $stylesheet = $opt{S} || '';
  $name_length = $opt{n} || 8;
  $chapter_cut_level = $opt{l} || 1;
  $preserve_chapter_word = $opt{p};
  $not_add_script = $opt{a};
  $ProductName = $opt{P} || 'Product';
  $EclipsePluginName = $opt{X} || $ProductName;

  $RemoveImgDimensions = 1;

  unless ($stylesheet) {
    # trying to find stylesheet
    my $dir = dirname($ARGV[0]) || '.';
    my (@css) = glob "$dir/*.css";
    if (@css == 1) {
      print STDERR "Found $css[0]. Will use it as a stylesheet.\n";
      $stylesheet = $css[0];
    }
    elsif (@css > 1)
    {
      print STDERR "Too many stylesheet files in the working directory. Unable to make a unique choice. Will not use any stylesheet.\n"
    }
  }

  $chapter_format = $opt{c};

  if ($chapter_format) {
    my $check;
    eval {
      $check = &format_file_name($chapter_format, 13, 'AChapterName');
    } 
    or die "Can't use format string $chapter_format!, $@";
    print STDERR "Check format: $check\n";
  }

  $do_blurb = $opt{B};
  
};

# hash for mapping of special symbols not recognized by several browsers
# to ASCII characters
# my %codes_mapping = (
#            "&#8209;" => "-",
#                  "&#146;" => "'",
#	    "&lt;" => "<",
#	    "&quot;" => "\"",
#	    "&gt;" => ">"
#);

# all tags that are separate 1st-level lists from each other should
# be defined here
my %stop_tag_names = (
                        h1 => 1,
                        h2 => 1,
                        h3 => 1,
                        h4 => 1,
          		h5 => 1
                    );

$center_img = 1;

# global counter for making anchor names unique.
$anchor_counter = 1;

@ARGV > 0 and -r $ARGV[0]  or  die "Can't read the input file $ARGV[0]\n";

# $dir -- input images directory
# $dir_encoded -- same, URL-encoded

if ($img_prefix) {
  ($dir = $ARGV[0]) =~ s/\.htm*$/_files/;
  ($dir_encoded = $dir ) =~ s/ /%20/g;
}

##
## Start parsing
##

my $doc = new HTML::TreeBuilder;

## If set to true, TreeBuilder will try to avoid creating ignorable whitespace text nodes in the tree. 
$doc->ignore_ignorable_whitespace(0);

## Parse text directly from a file. The $file argument can be a filename, an open file handle, or a reference to a an open file handle.
## In list context, returns the list of all $h's descendant elements, listed in pre-order 

print STDERR "Parsing $ARGV[0]...\n";
$doc->parse_file($ARGV[0]);

print STDERR +$doc->descendants." elements in\n";
&write_progress();

for my $e ($doc->descendants) { 
  if ( $e->{style} ) {
    $e->{style} =~ s/[\n\r]//g;
    $e->{class} = 'section-break' if $e->{style} =~ /section-break/;
    delete $e->{style};
  }
  delete $e->{color};
  delete $e->{size};
}

&write_progress();

# delete all specified tags
$_->delete                       for  $doc->find_by_tag_name(qw( del style s ));

&write_progress();
 
# delete all specified tags and push their content to upper level
$_->replace_with_content->delete for  $doc->find_by_tag_name(qw( ins u ));

&write_progress();

# delete redundant <font>s
for my $e ($doc->find_by_tag_name('font')) {
  delete $e->{face} unless  lc($e->{face}) eq 'symbol';
  unless ( $e->all_external_attr_names ) 
   { $e->replace_with_content->delete }
}

&write_progress();

# skim empty spans
for my $e ($doc->find_by_tag_name('span')) {
  unless (grep {ref $$_} $e->content_refs_list)
  {
    for my $c ($e->content_refs_list) {
      if ($$c =~ /^[\s\xa0]*$/) {$$c = ''};
    }
  }
}

&write_progress();

# delete <span>'s 
for my $e ($doc->find_by_tag_name('span')) {
  # remove lang=EN
  delete $e->{lang}  if  (exists $e->{lang} and $e->{lang} =~ /^EN/i);
  
  unless ( $e->all_external_attr_names ) {
    $e->replace_with_content->delete; 
  }
}

&write_progress();

# delete <i> inside <em>. 
for my $h ($doc->find_by_tag_name('i')) {
  $h->replace_with_content->delete  if  $h->parent->tag eq 'em';
}


&write_progress();

# filter text
$doc->traverse( 
sub {
  my ($elt, $start, $depth,$text_parent,$text_ind) = @_;
  return 1 if ref $elt;
  
  $elt =~ s{\</?o\:p\>}{}g;
  $elt =~ s{\<!.*?\>}{}sg;

  if (ref $text_parent) {
    $text_parent->content->[$text_ind] = $elt;
  }
  else { print STDERR "Orphaned text: $elt\n"; }
  return 1
},
0);

&write_progress();

# cleans img tags
# moves all images to the specified deirectory (if any)
for my $h ($doc->find_by_tag_name('img')) {
  delete $h->{'v:shapes'};
  
  #  my $align;
  
  # Patch for NN: if <img> is inside of <p> then NN displays the text after <img> incorrectly
  # this patch removes surrounding <p> tag for every <img>
  foreach ($h->lineage) 
  {
    next unless (ref);
    if ($_->tag eq 'p')
    {
        ## do the work only if <img> is the only tag inside <p> 
        #print STDERR +$_->descendants . ' ' . collect_text($_) . "\n";
        last  if (($_->descendants > 0) or (&collect_text($_) =~ /^\s*$/)); 
        
        my $align = $_->{align};
        
        $_->replace_with($h);
        
        if ($align)
        {
          my $div_tag = HTML::Element->new("div", 'align' => $align);
          $h->preinsert($div_tag);
          $h->detach;
          $div_tag->push_content($h);
        }
        last;
    }
  }
  
  if ($img_prefix)
  {
    if ( exists $h->{'src'} ) {
      # added 'i' next 2 lines
      $h->{'src'} =~ s#\Q$dir_encoded\E[\/]#$img_prefix#sgoi;
      push @images, ( $h->{'src'} =~ m#\Q$img_prefix\E([^"\s]+)#sgoi );
    }
    else {
      print STDERR "No src in img !!!\n";
      $h->{'src'} = "$img_prefix/unknown.png";
    }
  }

  if ($RemoveImgDimensions)
  {
    delete $h->{width};
    delete $h->{height};
  }
}

&write_progress();

for my $h ($doc->find_by_tag_name('p')) 
{
  my @children = $h->descendants;
  &img_in_p($h) if ( @children == 1 
                 and $children[0]->tag eq 'img' 
                 and &collect_text($h) =~ /^\s*$/ );
}

&write_progress();

## delete empty paragraphs
# \xa0 == &nbsp;
PARA: 
for my $e ($doc->find_by_tag_name('p')) {
  for ($e->content_list) { 
    next PARA if ref;
    next PARA unless /^[\s\xa0]*$/; 
  }
  if ($e->parent->tag eq 'td') { $e->preinsert("\xa0") };
  $e->delete;
}

&write_progress();

# Aligns images as specified
sub img_in_p {
  $_[0]->tag('div');
  defined $center_img or return 1;
  if ($center_img =~ /\d/) {
   for ($_[0]->find_by_tag_name('img')) {
     $_->{'hspace'} = $center_img;
   }
   delete $_[0]->{'align'};
  }
  elsif ($center_img =~ /center|left|right|justify/) {
   $_[0]->{align} = $center_img;
  }
  1;
}



# Returns first non-text left sibling, undef if none
sub left_ref {
  my $self = shift;
  for ( reverse $self->left ) {
    return $_ if ref;
  }
  undef
}



# Returns first non-text right sibling, undef if none
sub right_ref {
  my $self = shift;
  for ( $self->right ) {
    return $_ if ref;
  }
  undef
}

# remove section breaks 
# at this point <body> <div> <br> <div>
for my $h ( $doc->look_down( _tag => 'br', 
                         class => 'section-break'
    ) ) 
{
  my ($div_before, $div_after) = (left_ref($h), right_ref($h));
  next unless 
    defined $div_before and defined $div_after and 
    $div_before->tag eq 'div' and $div_after->tag eq 'div';
  
  $div_before->push_content( $div_after->detach_content );
  $div_after->delete;
  $h->delete;
}
# at this point <body> <div + div>

&write_progress();

# delete containers that contain chapters
for my $h ($doc->find_by_tag_name(qw( h1 h2 h3 h4 h5 h6 ))) 
{
# Returns the list of $h's ancestors, starting with its parent, 
# and then that parent's parent, and so on, up to the root. 
# If $h is root, this returns an empty list.
  for ( reverse $h->lineage ) 
  {
    if ( $_->parent and $_->parent->tag ne 'body' 
     and $_->tag =~ /^(?:div|table)$/ ) 
    { 
      $h->detach;
      $_->replace_with($h)->delete;
      last;
    }
  }
}

# correcting table appearance
for $h ($doc->find_by_tag_name('table'))
{
  $h->{border}      = '1';
  $h->{cellspacing} = '0';
  $h->{cellpadding} = '0';
}

# correcting table row appearance
for $h ($doc->find_by_tag_name('tr'))
{
  $h->{bgcolor}     = '#E6E6E6';
  $h->{bordercolor} = '#ffffff';
}


&write_progress();

# constants to keep "class" attribute or get rid of it
use constant KEEP_CLASS => 0;
use constant DROP_CLASS => 1;

$cur_list_depth = 0;
@list_containers = ();


# MS Classes =>  HTML tags
# this hash specifies what to do when spcified class envountered
%class_convert = (
  MsoListBullet =>  \&list_conv,
  MsoListNumber =>  \&list_conv, 
  MsoListBullet2 =>  \&list_conv,
  MsoListNumber2 =>  \&list_conv,
  MsoListBullet3 =>  \&list_conv,
  MsoListNumber3 =>  \&list_conv,
  MsoListBullet4 =>  \&list_conv,
  MsoListNumber4 =>  \&list_conv,
  MsoListBullet5 =>  \&list_conv,
  MsoListNumber5 =>  \&list_conv,
  MsoListBullet6 =>  \&list_conv,
  MsoListNumber6 =>  \&list_conv,
  MsoListBullet7 =>  \&list_conv,
  MsoListNumber7 =>  \&list_conv,
  MsoListBullet8 =>  \&list_conv,
  MsoListNumber8 =>  \&list_conv,
  MsoListBullet9 =>  \&list_conv,
  MsoListNumber9 =>  \&list_conv,
  Steps => \&list_conv,
  Note => sub { 
    my $h = shift;
    my ($dum,$first) = $h->look_down(sub { &collect_text($_[0])=~/^note/i });
    $first or return 0;
    for ($first->content_refs_list) { 
      next if ref $$_;
      $$_ =~ s/\xa0+/ / and last;
    }
    0;
  },
  Code      => sub { $_[0]->tag('code') },

  CodeLines => sub {
    $_[0]->attr('class' => 'codelines'); 
    KEEP_CLASS;

  },

  ListCode => sub {
    $_[0]->attr('class' => 'listcode'); 
    KEEP_CLASS;

  },

  ListCode2 => sub {
    $_[0]->attr('class' => 'listcode');
    KEEP_CLASS;

  },

  ListCode3 => sub {
    $_[0]->attr('class' => 'listcode'); 
    KEEP_CLASS;

  },

  Emphasis2 => sub { $_[0]->tag('strong') },
  PreformattedText => sub {  
                        my $h = shift;
                        $h->tag('pre');
                        for ($h->content_refs_list) {
                          next if ref $$_;
                          HTML::Entities::encode($$_,'<>&');
                        }
                        $h->unshift_content(" ");#(['br']);# kludge
                        DROP_CLASS;
                      },
  picture0 => \&img_in_p,
  Picture => \&img_in_p,
  MsoCaption => sub { 
                  $_[0]->attr('class'=> 'caption'); 
                  for ($_[0]->find_by_tag_name('i','b')) {
                    $_->replace_with_content->delete }
                  KEEP_CLASS;
                },
  MsoListContinue => sub { 
                  $_[0]->attr('class'=> 'listcontinue'); 
                  KEEP_CLASS;
                },
  Bold => sub { $_[0]->tag('strong'); 
                defined $_[0]->content and ref $_[0]->content->[0] or return 1;
                if ( $_[0]->content->[0]->tag =~ /b|strong/ ) { 
                  $_[0]->content->[0]->replace_with_content->delete }
                DROP_CLASS;
              },
  TableHead => sub {
                 if ($_[0]->parent->tag eq 'td') {
                  $_[0]->parent->tag('th');
                 }
               },
  Functions => sub { 
                 $_->replace_with_content->delete 
                  for $_[0]->find_by_tag_name(qw(font b i));
                 KEEP_CLASS; },
  Tabletextbulleted => \&table_list_conv,
  Tabletextnumbered => \&table_list_conv,
  TocHeading => sub { $_[0]->delete },
  MsoToc1 => sub { $_[0]->delete },
  MsoToc2 => sub { $_[0]->delete },
  MsoToc3 => sub { $_[0]->delete },
  MsoToc4 => sub { $_[0]->delete },
  MsoNormal => \&process_normal_text,
  MsoBodyText => \&process_normal_text
);

#  MsoNormal => sub { $cur_list_depth = 0 }

&write_progress();

# removes all anchors that begin with "ref" or "toc"
# these anchors will be later restored with handy names
&remove_their_anchors;
&write_progress();

#my $outer_list;

# search for all tags with specified class handlers
# execute the handler when encountered registered class
{
  my $counter = 0;
  for my $h ( $doc->descendants ) 
  {
    $cur_list_depth = 0    if (ref $h and $h->tag and defined $stop_tag_names{$h->tag});
    if ( exists $h->{class} and exists $class_convert{$h->{class}} ) 
    { 
      my $handle = $class_convert{$h->{class}};
      delete $h->{class}    if ($handle->($h));
      &write_progress()    if (++$counter % 10 == 0);
    } 
  }
}

&write_progress();

# processes "normal" text with classes "MsoNormal" or
# "MsoBodyText" in the following way:
# if paragraph is in the table then it preserved as is and
# no chages are made to it,
# if the paragraph NOT in the table then all lists are
# finished and the "Normal" text appears outside of any list.
sub process_normal_text
{
    my $parent = $_[0]->parent;
    my $in_table = 0;
    while ($parent)
    {
        if ($parent->tag eq 'table')
        {
            $in_table = 1;
            last;
        }
        $parent = $parent->parent;
    }
    $cur_list_depth = 0    if (not $in_table);
}


# types of lists:
use constant UNKNOWN_LIST => 0;
use constant NUMBERS_LIST => 1; # 1. 2...
use constant LOWERCASE_LIST => 2; # a. b...
use constant UPPERCASE_LIST => 3; # A. B...
use constant SMALL_ROM_LIST => 4; # i. ii...
use constant LARGE_ROM_LIST => 5; # I. II...

# Common class handler. Makes nested lists from
# MsoListBullet and MsoListNumber paragraphs
sub list_conv
{
    my $h = shift;
    my($list_tag,$list_depth) = ($h->{class} =~ /MsoList(Bullet|Number)([0-9]*)/g);
        
    unless ($list_tag) 
    {
        my $t = &collect_text($h);
        $list_tag = $t=~/^\d/ ? 'Number' : 'Bullet';
    }
    $list_tag = {Bullet=>'ul', Number=>'ol'}->{$list_tag};
    $list_depth ||= 1;
    
    my $list_container;
    my $list_item_number;
    my $value;

    # find out the ordinal number of list item    
    $list_item_number = &get_list_item_number(\$h)    if ($list_tag eq 'ol');

    $h->normalize_content;
    # collect downer siblings - until beginning of next list
    # or some "stop tag" encountered
    my @h_right_siblings = &get_right_siblings($h);
    my $use_siblings = 0;
    if ($list_depth > $cur_list_depth)
    {
        # starting new list
        $list_container = &create_list_container($h, $list_tag, $list_depth);
        
        $value = &get_list_item_value($list_tag, $list_item_number);
        
        my $list_type = &get_list_type($value);
        $list_container->{type} = $list_type    if ($list_type);
    }
    else
    {
        # continuing already started list
        if (($list_tag eq 'ol') and ($list_item_number) and 
            ($list_item_number =~ /^1\.|^a\.|^A\./))
        {
            # starting new ordered sublist
            $list_container = &create_list_container($h, $list_tag, $list_depth);
            
            $value = &get_list_item_value($list_tag, $list_item_number);
        }
        elsif (${$list_containers[$list_depth]}->tag ne $list_tag) 
        {
            # starting new list if list type changed
            $list_container = &create_list_container($h, $list_tag, $list_depth);
            
            $value = &get_list_item_value($list_tag, $list_item_number);
        }
        else
        {
            # continuing already started list on the same level
            $value = &get_list_item_value($list_tag, $list_item_number);
            
            $list_container = ${$list_containers[$list_depth]};
            my $list_type = &get_list_type($value);
            $list_container->{type} = $list_type    if ($list_type);
            
            foreach (@h_right_siblings)
            {
                $_->detach;
            }
            $use_siblings = 1;
        }
    }

    $h->detach;
    # create actual <li> tag
    ($value and $list_container and $value =~ /\d/) ? 
        $list_container->push_content(['li', { value => $value }, $h]) :
        $list_container->push_content(['li', $h]);
    # put all non-list tags into the list tag
    $list_container->push_content(@h_right_siblings)    if ($list_container);
    
    $cur_list_depth = $list_depth;
    
    # get rid of class attribute
    DROP_CLASS;
}

# Returns all right siblings until beginning of next list 
# or some "stop tag" encountered
sub get_right_siblings
{
    my $h = shift;
    my @siblings = ();
    foreach ($h->right)
    {
        next    unless ($_ and ref $_);
        return @siblings    if (exists $_->{class} 
                                and ($_->{class} =~ /^MsoList(Bullet|Number)/ or
                                     $_->{class} =~ /^MsoNormal/ or 
                                     $_->{class} =~ /^MsoBody/));
        return @siblings    if (defined $stop_tag_names{$_->tag});
        push @siblings, $_;
    }
    return @siblings;
}

# Creates ordered or unordere list container.
# Places created element into the internal cache of
# list containers
sub create_list_container
{
    my ($h, $list_tag, $list_depth) = @_;
    
    my $list_container = new HTML::Element $list_tag;
    $list_containers[$list_depth] = \$list_container;
    if ($list_depth > 1)
    {
        ${$list_containers[$list_depth - 1]}->push_content($list_container);
    }
    else
    {
        $h->preinsert($list_container);
    }
    
    return $list_container;
}



# Returns ordinal number of list item from given string.
# e.g. if input string is "1. item 1" then this method will return "1."
sub filter_value
{
    my $value = shift;
    
    return undef    unless ($value);
    
    $value =~ s/[^0-9a-zA-Z.]//g;
    $value =~ /(?:(^[0-9]+)\.$)|(?:(^[a-z]+)\.$)|(?:(^[A-Z]+)\.$)/;
    if ($1) { $value = $1; }
    elsif ($2) { $value = $2; }
    elsif ($3) { $value = $3; }
    
    return $value;
}



# Returns the type of the list based on first symbols in the list body
sub get_list_type
{
    my $symbol = shift;
    
    return (UNKNOWN_LIST, undef)    unless $symbol;
    
    if ($symbol =~ /^\d/)
    {
      return (NUMBERS_LIST, '1');
    }
    elsif ($symbol =~ /^[a-hj-z]/)
    {
        return (LOWERCASE_LIST, 'a');
    }
    elsif ($symbol =~ /^[A-HJ-Z]/)
    {
        return (UPPERCASE_LIST, 'A');
    }
    elsif ($symbol =~ /^i/)
    {
        return (SMALL_ROM_LIST, 'i');
    }
    elsif ($symbol =~ /^I/)
    {
        return (SMALL_ROM_LIST, 'I');
    }
    
    return (UNKNOWN_LIST, undef);
}

# Gets the number from list item.
# e.g. if list item is "c. the 3rd item" then
# this method will return "c." as list number
sub get_list_item_number
{
    my $href = shift;
    my $h = $$href;
    my $list_item_number;
    
    for ( $h->content_refs_list ) 
    { 
        my $t = &collect_text($$_);
        $t =~ s/[\s\xa0]//g; # !!! 
        $list_item_number = $t    if $t=~/(?:^[0-9]+\.$)|(?:^[a-z]+\.$)|(?:^[A-Z]+\.$)/; # Now: expect "N." or "a." or "A."
        
        if ($t =~ /^\w{1,5}\.$/)
        {
            if ( ref $$_ ) # otherwise, delete the node with list symbol ("1." or "a." etc)
            { 
                $$_->delete 
            } 
            else 
            { 
                $$_ = '' 
            }
            last
        }
    }
    return $list_item_number;
}

# This method makes sure that current list type allows
# specifying "value" attrribute for their items.
# ordered lists allows this but unordered don't allow.
# Returns filtered value of "value" attribute or undef.
sub get_list_item_value
{
    my ($list_tag, $list_item_number) = @_;
    
    return (($list_tag eq 'ol') and ($list_item_number)) ? 
        &filter_value($list_item_number) : 
        undef;
}

# Converter for "Tabletextbulleted" class.
# makes all list items as separate lists to 
# avoid problem of searching for the end of 
# list.
# Could be extended to operate with other type of
# in-table lists.
sub table_list_conv
{
    my $h = shift;
    my ($ul, $value);

    my($list_tag) = ($h->{class} =~ /Tabletext(bulleted|numbered)/g);

    $list_tag = {bulleted=>'ul', numbered=>'ol'}->{$list_tag};

    my $list_item_number = &get_list_item_number(\$h)    if ($list_tag eq 'ol');
    $value = &get_list_item_value($list_tag, $list_item_number);
    
    $h->preinsert( $ul = new HTML::Element $list_tag );
    
    $h->detach;

    ($value and $ul and $value =~ /\d/) ? 
        $ul->push_content(['li', { value => $value }, $h]) :
        $ul->push_content(['li', $h]);
}


# apply stylesheet to the document
&style($doc, $stylesheet);

&write_progress();

if ( $img_prefix ) {
  # creates images directory and copies images into it (if any)
  my ( $base, $img_dir) = fileparse $img_prefix;
  if ($img_dir) {
    mkdir $img_dir, 0755 or warn "Can't create directory '$img_dir': $!" 
        unless -e $img_dir;
  }
  copy("$dir/$_", "$img_prefix$_") for @images;
}

&write_progress();

# collect and adds necessary anchors
&add_anchors;

&write_progress();

# adds javascript decorators to the links
&decorate_links unless $opt{'u'};

&write_progress();

# finds and collects chapters in the document
&measure_chapters if $chapter_format;

&write_progress();

# make table of content
&ToC if $toc_file;

&createTreeStructureJs;

&createEclipseFiles;

&write_progress();
print STDERR "\n";

# write content of chapters into separate files
&cut_to_chapters if $chapter_format;

# if there's no need in cutting chapters,
# print the output document away.
print &output($doc) unless $chapter_format; 

print STDERR +$doc->descendants." elements out\n";

&process_templates;

$doc->delete;


### subroutines




# Applies specified stylesheet to the document. 
# Adds "link" tag into the "head" tag
sub style { 
  my ($h,$css) = @_;
  return unless $css;
  $h->{'_head'}->push_content(['link', 
      { rel => 'stylesheet', href => $css, type => 'text/css'}]) 
}



# Adds "blurb" (full contents of command line) as comment into the output 
# file
sub add_blurb {
  $_->unshift_content( 
    HTML::Element->new ('~comment', 'text' => $command_line) 
  ) for @_;
}
  


# Adds javascript decoration to the link. Instead of raw links in the
# status bar of the browser, link names will be shown
sub decorate_links {
  my %links;
  for my $h ($doc->find_by_tag_name('a')) {
    if (exists $h->{'name'}) { 
      my $text = ''; 
      for ($h, $h->lineage) { 
        last if ( $_->tag eq 'body' or $_->root );
        $text = &collect_text($_); 
        last if $text=~/\S/ ;
      }
      $links{$h->{'name'}} = $text if $text =~ /\S/;
    }
  }
  for my $h ($doc->find_by_tag_name('a')) {
    if (exists $h->{'href'}) {
      my ($name) = $h->{'href'} =~ /#(.*)$/g;
      next unless $name and exists $links{$name} and $links{$name} =~/\S/;
      $h->{'onmouseover'} = "self.status='$links{$name}';return true;";
      $h->{'onmouseout'} = "self.status='';";
    }
  }
}




# Removes generated by MS Word anchors with long names beginning with
# "Toc" or "Ref"
sub remove_their_anchors {
  for my $h ($doc->find_by_tag_name('a')) {
    $h->replace_with_content->delete  
      if  defined($h->{name}) && $h->{name} =~ /^_(?:Toc|Ref)/;
  }
}



# Adds anchor tag to every heading paragraph.
# Places created anchors to the internal cache.
sub add_anchors {
  for my $h ($doc->find_by_tag_name( qw(h1 h2 h3 h4 h5 h6) )) {
    
    $_->replace_with_content->delete  for  $h->find_by_tag_name(qw(font b i));
    
    my $title  = &collect_text($h);
    next unless $title=~/\S/;
    my $anchor = &anchorize($title);
    while (exists $Anchor{$anchor}) {  
      # `($1||0)' instead of `$1' to avoid complaints about ''+1
      $anchor =~ s/([0-9]*)$/($1||0)+1/e  
    } 
    $h->unshift_content(['a', {name => $anchor}]);
    $title =~ s/(['"])/\\$1/g;
    $Anchor{$anchor} = { chapter => 'main', desc => $title };
    $HeadAnchor{$h} = $anchor;
  }
}


use constant TOC_HEADERS =>  qw(h1 h2 h3) ;

# Creates table of content
#
# we have 3 kinds of ToC now -- plain old toc.html and TreeStructure.js 
# and Eclipse's toc.xml
sub ToC 
{
  my $toc = new HTML::TreeBuilder;
  $toc->{_head}->push_content(['title', 'Contents']);
  
  # apply stylesheet and blurb, if any
  &style($toc, $stylesheet);
  &add_blurb($toc) if $do_blurb;
  
  my $cur_level = 0;
  my $ul = $toc->{_body};
  for my $h ($doc->find_by_tag_name( TOC_HEADERS )) {
    my $title  = &collect_text($h);    
    next unless $title=~/\S/;
    my $anchor = $HeadAnchor{$h};
    my $head_level = substr $h->tag, 1;

    # make the TOC as nested list.
    # creates TOC items as list items
    while ( $head_level > $cur_level ) {
      $ul->push_content( ['ul', {class=>'toc'}] );
      my $ult = $ul->content->[-1];
      $ult->parent($ul);
      $ul = $ult;
      $cur_level++;
    }
    while ( $head_level < $cur_level ) {
      $ul = $ul->parent;
      $cur_level--;
    }

    # actual creation of TOC item with decorated link
    # these links are decorated even if no decoration was specified
    # in command line
    $ul->push_content( 
      ['li', 
        ['a', 
          { href => $Anchor{$anchor}{chapter}.".html",
            target => 'desc',
            onmouseover => "self.status='$Anchor{$anchor}{desc}';return true;", 
            onmouseout => "self.status='';" },
          $title ] 
      ] );
  }
  
  ( open TOC, ">$toc_file"  and  print TOC &output($toc) ) 
    or warn "cannot create ToC";
}

sub createTreeStructureJs
{
  my $text = q{
// You can find instructions for this file here:
// http://www.treeview.net

// Decide if the names are links or just the icons
USETEXTLINKS = 1;  //replace 0 with 1 for hyperlinks

// Decide if the tree is to start all open or just showing the root folders
STARTALLOPEN = 0; //replace 0 with 1 to show the whole tree

ICONPATH = 'icons/'; //change if the gif's folder is a subfolder, for example: 'images/'
  };

  $text .=  qq{foldersTree = gFld("$ProductName", "");\n};

  my @heads = $doc->find_by_tag_name( TOC_HEADERS );
  for (0 .. (@heads-1))
  {
    my $h = $heads[$_];
    
    my $title  = &collect_text($h);    
    next unless $title=~/\S/;
    my $anchor = $HeadAnchor{$h};
    my $head_level = substr $h->tag, 1;
    my $next_level = substr $heads[$_ + 1]->tag, 1  if $heads[$_ + 1];
    
    my $aref = $Anchor{$anchor};
    $text .= do {
      if ($head_level == 1 or ($next_level and $next_level > $head_level) )
      {
        my $parent = $head_level == 1 ? 'foldersTree' : "aux" . ($head_level - 1);
        qq{aux$head_level = insFld($parent, gFld("$title", "$aref->{chapter}.html#$anchor"));\n};
      }
      else
      {
        my $parent = "aux" . ($head_level - 1);
        qq{insDoc($parent, gLnk("R", "$title", "$aref->{chapter}.html#$anchor"));\n};
      }
    };
  }

  open my $fh, ">TreeStructure.js" or warn "cannot create TreeStructure.js";
  $fh and $fh->print($text);
}


=head2 Eclipse integration

Option C<-X> is used to create xml files for Eclipse help:
  toc.xml
  topics_Ch1.xml
  topics_Ch2.xml
  ...

=cut

# toc.xml and plugin.xml are created automatically from the templates
# now we need to make topics_ChNN.xml
# should really use some xml builder ...
sub createEclipseFiles
{
#  my $templateDir = getTemplateDir . '/eclipse';
  unless (-d 'eclipse')  {
    mkdir 'eclipse';
  }
  
  my $i = 1;
  my $text;
  my $previous_level = 0;
  my $fh;
  for my $ch (@Chapters)
  {
    my $level = substr $ch->{h1}{_tag}, 1;

    # close tag
    if ($level <= $previous_level)
    {
      $text .= "</topic>\n" for ($level .. $previous_level - 1);
      if ($level == 1)
      {
        $text .= "</toc>\n";
        $fh->print($text) if $fh;
        $text = '';
      }
      else
      {
       $text .= "</topic>\n";
      }
    }
    
    if ($level == 1)
    {
      open $fh, ">eclipse/topics_Ch$i.xml";
      unless ($fh) {
        warn "Can't open eclipse/topics_Ch$i.xml for writing. Skipping the rest."; 
        last}
      
      $i ++;
      $text .= q{<?xml version="1.0" encoding="UTF-8"?>
<?NLS TYPE="org.eclipse.help.toc"?>
};

      $text .= qq[<toc label="$ch->{title}">\n];
    }
    else
    {
      $text .= qq[<topic label="$ch->{title}" href="$ch->{condensed}.html">];
    }

    $previous_level = $level;
  }

  # close tag
  $text .= "</topic>\n" for (1 .. $previous_level - 1);
  $text .= "</toc>\n" ;

  $fh->print($text) if $fh;

}

# Creates the link name from the link's target text.
# Gets rid of special or non-printed symbols.
# Generates unique name to avoid conflicts with other links even to the
# same text.
sub anchorize {
  my $ret = shift;
  $ret =~ s/[][{}" ,.:;?!'\/()\n\r\xa0-]+/_/g;
  $ret =~ s/(^_+)|(_+$)//g;
  $ret =~ s/\&#(?:\d)+//g; # get rid of "&#xxxx" symbols - links consists of such symbols will not work in NN
  $ret .= $anchor_counter++;
  lc $ret;
}



# Collects all text elements from the input parameter
sub collect_text {
  return join(' ', map( (ref($_) ? $_->as_text : $_) , @_));
}



# Collects chapters from the document.
# Assumes that each chapters begins from "h1" tag.
# Composes cross-chapter links
sub measure_chapters 
{
  my @headers_to_cut = map {"h$_"} reverse (1 .. $chapter_cut_level);
  
  for my $h ($doc->find_by_tag_name(@headers_to_cut)) 
  {
    my $title = &collect_text($h);
    next unless $title =~ /\S/;
    $chap_n++;
    my $chapter_name = &format_file_name($chapter_format, $chap_n, &condense($title));

    # check length and uniqueness
    $chapter_name = check_length( $chapter_name, map {$_->{condensed}} @Chapters);

    my @content = ();
    # for all tags from the beginning and down
    for my $hh ($h, $h->right) {
      last if ( $hh ne $h and ref($hh) and $hh->find_by_tag_name(@headers_to_cut) );
      push @content, $hh;
      
      next unless ref $hh;

      for ($hh->find_by_tag_name('a')) {
        next unless exists $_->{name};
        $Anchor{$_->{name}}{chapter} = $chapter_name;
      }
      
    }

    # creates record in Chapters cache    
    push @Chapters , { title => $title, 
                       condensed => $chapter_name,
                       h1 => $h, 
                       content => [@content] };
  }

  # creates cross-chapter links  
  for ($doc->find_by_tag_name('a')) {
    next unless ( exists $_->{href}  and  $_->{href} =~ /^#/ );
    my $name = substr $_->{href}, 1;
    defined $Anchor{$name}{chapter} or  
      $Anchor{$name}{chapter} = "won't be used?";
    $_->{href} = $Anchor{$name}{chapter}.'.html'.$_->{href}
  }
}



# Returns name of file that is not longer that N symbols.
# N is specified as command-line parameter and by default is 8.
sub check_length
{
  my ($name, @names) = @_;
  my %name_hash = map { $_=> 1 } @names;
  $name = substr $name, 0, $name_length;

  # check uniqueness
  my $cnt = 1;
  while ( exists $name_hash{$name} )
  {
    substr($name, 0 - length("$cnt")) = $cnt;
    $cnt ++;
  }
  return $name;
}



# Performs actual cutting chapters by the files.
# Creates chapter files and writes the chapter content to it.
# Adds necessary HTML header/footer tags
sub cut_to_chapters 
{
  for my $h (@Chapters) {
    my $ch = new HTML::TreeBuilder;
    &style($ch, $stylesheet);
    $ch->{_head}->push_content( ['title', &map_codes_to_symbols($h->{title})] );
    $ch->{_head}->push_content( ['script', qq(var file_name="$h->{condensed}.html")] )	unless ($not_add_script);
    &add_blurb($ch) if $do_blurb;

    for ( @{$h->{content}} ) {
      $ch->{_body}->push_content(ref($_) ? $_->clone : $_);
    }

    $ch->{_body}->push_content(['br', {clear => 'all'}]);

    open CH,  ">$h->{condensed}.html" or die;
    print "$h->{condensed}.html\n";
    print CH &output($ch);
    close CH;
    $ch->delete;
  }
}



# Returns StudlyCapped string.
# If input string is "this is a string" then this method will return
# "ThisIsAString".
# Also cuts off punctuation marks and non-printable symbols
sub condense 
{
  my $t = shift;
  $t = join '', map(ucfirst, split(/[[:space:][:punct:]\xa0]/,$t));
  $t =~ s/^(?:chapter[0-9]+|appendix[0-9a-zA-Z])//i    unless (defined $preserve_chapter_word);
  $t;
}



# Formats name for chapter file as specified in command line parameters
sub format_file_name 
{
  my ($format, $num, $string) = @_;
  my @arg_list = map { /[udi]$/ ? $num : $string } ($format =~ /(%[0-9.#+ -]*[sudi])/g);
  sprintf $format, @arg_list;
}




# Maps symbols that are displyable by several browsers to ASCII symbols
sub map_codes_to_symbols
{
    my $text = shift;
    my ($key, $value);
    while (($key, $value) = each %codes_mapping)
    {
        $text =~ s/$key/$value/g;
    }

    return $text;
}



# Writes progress mark as dot symbol
sub write_progress
{
    print STDERR '.';
}



# Returns true if input parameter is phrase markup tag, such as
# "span", "b", "i" etc
sub indent_p { 
  my $tag = shift;
  not $HTML::Tagset::isPhraseMarkup{$tag} 
}


# Writes out the document with indentation
sub output {
  my $self = shift;

  my %BLANK_LINE_BEFORE = map {$_=>1} 
    qw(div h1 h2 h3 h4 table ul ol p head body script);

  my @html = ();
  # for all elements..  
  $self->traverse( sub {
    my($node, $start, $depth, $tp, $ti) = @_;
    if (ref $node) {
    
      # solves problem with extra columns
      return 1 if ($node->tag eq 'td' and $node->{_implicit});

      my $tag = $node->tag;
      my $tag_indent = indent_p($tag);
      if ($start) {
        push @html,  
          $BLANK_LINE_BEFORE{$tag} ? "\n" : '',
          !$node->parent || 
            ($tag_indent && $node->pindex != 0) ? "\n" : '' ,
          $tag_indent ? '  'x$depth : '' ,
          $node->starttag,
          $tag_indent ? "\n" : '' ;
      } else { 
        push @html,  
          $tag_indent ? "\n" . '  'x$depth : '' ,
          $node->endtag,
          $BLANK_LINE_BEFORE{$tag} ? "\n" : '';
      }
    } else {
       
      HTML::Entities::encode($node) unless  $tp->tag eq 'script' ;
      
      # convert special symbols  
      $node =~ s/\&amp;#/\&#/g;
      $node =~ s/\&middot;//g;
      # $node =~ s/\&nbsp;//g;
      $node = &map_codes_to_symbols($node);
              
      if ( length $node > 72 and $tp->{_tag} ne 'pre') {
        $node =~ s/(.{0,72})\s+/$1\n/g;
        # good ennough replacement of Text::Wrap::wrap
      }
      push @html, $node;
      
    }
  }
  );
  my $ret = join('', @html, "\n");
  # final text adjustments
  $ret =~ s/\n\n+/\n\n/gs; 
  $ret =~ s/\n\n([<a-z])/\n$1/gsi;
  $ret;
  
}


=head2 Templates

Processes files in templates/ and/or copies them to the working directory.

  Convert.pl
  templates/
    title.html
    *
    etc...

=cut

use Cwd;
use Text::Template 'fill_in_file';


sub process_templates
{
  my $templateDir = &getTemplateDir;
  return if ( ! -e $templateDir );

  my %TEMPLATE_COPY = ( 
    'plugin.properties' => 'eclipse/plugin.properties',
    'plugin.xml' => 'eclipse/plugin.xml',
    'toc.xml' => 'eclipse/toc.xml'
  );

  my %vars = (
    Chapters => \@Chapters, 
    ProductName => $ProductName,
    EclipsePluginName => $EclipsePluginName,
  );
    
  my $cwd = cwd();
  chdir $templateDir;
  
  for my $file (glob "*")
  {
    if (-d $file)
    {
      # copy
      &copyDir($file, "$cwd/$file")
    }
    else
    {
      my $text = fill_in_file($file, HASH => \%vars, DELIMITERS => ['[%', '%]'])
        or warn "cannot fill in $file: $!";

      if ($text) 
      {
        my $dest = exists $TEMPLATE_COPY{$file} ? $TEMPLATE_COPY{$file} : $file;

        open my $fh, ">$cwd/$dest" or next;

        $fh->print($text);
      }
      else
      {
        # copy
        copy($file, "$cwd/$file") 
      }
    }
  }

  chdir $cwd;
}

sub getTemplateDir
{
  my $templateDir = $0;

  if ($templateDir =~ /[\/\\]/) {
    $templateDir =~ s{[/\\]([^/\\]*)$}{/templates}; 
  }
  else {
    $templateDir = 'templates';
  }
  
  $templateDir;
}

sub copyDir
{
  my ($from, $to) = @_;
  mkdir $to;
  $to =~ s/$from$//;
  for my $file (glob "$from/*")
  {
    if (-d $file) { &copyDir($file, "$to/$file") }
    else          { copy($file, "$to/$file") }
  }
}

=head1 Changes

=over 4

=item v1.9

Correct inline images. 
Processing the 'templates' directory.
New parameter '-P' for product name.

=item v1.next

Eclipse integration. Corrected list handling (trim spaces and nbsp from
probable item index).

=back

=head1 AUTHOR

Vadims Beilins (I<vadims_beilins@exigengroup.lv>)

Sergey Denisov (I<sergey_denisov@exigengroup.com>)

=cut


